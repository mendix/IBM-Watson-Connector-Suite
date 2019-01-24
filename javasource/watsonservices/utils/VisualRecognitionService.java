package watsonservices.utils;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.service.exception.BadRequestException;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImage;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifierResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Classifiers;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.CreateClassifierOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DeleteClassifierOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectFacesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectedFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageWithFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ListClassifiersOptions;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.Image;
import watsonservices.proxies.Classifier;
import watsonservices.proxies.ClassifierClass;
import watsonservices.proxies.TrainingImagesZipFile;
import watsonservices.proxies.VisualRecognitionImage;

public class VisualRecognitionService {

	private static final String WATSON_VISUAL_RECOGNITION_LOGNODE = "WatsonServices.IBM_WatsonConnector_VisualRecognition";
	private static final ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_VISUAL_RECOGNITION_LOGNODE).toString());
	private static final String CLASSIFIER_ENTITY_NAME = Classifier.entityName;
	private static final String CLASSIFIER_ENTITY_PROPERTY = Classifier.MemberNames.ClassifierId.name();
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPG = "jpg";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPEG = "jpeg";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG = "png";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_GIF = "gif";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_TIF = "tif";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_TIFF = "tiff";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP = "zip";
	private static final String WATSON_VISUAL_RECOGNITION_VERSION_DATE = "2018-03-19";

	public static List<IMendixObject> classifyImage(IContext context, String apikey, String url, VisualRecognitionImage VisualRequestObject, List<Classifier> classifiers) throws MendixException {
		LOGGER.debug("Executing ClassifyImage Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		validateImageFile(context, VisualRequestObject);
		if (classifiers == null || classifiers.isEmpty()) {
			LOGGER.error("ClassifyImage needs at least one classifier to be specified");
			throw new MendixException("No classifiers are specified");
		}

		final InputStream imageInputStream = new RestartableInputStream(context, VisualRequestObject.getMendixObject());

		final ClassifyOptions options = buildClassifyImagesOptions(classifiers, imageInputStream, VisualRequestObject.getName());
		final ClassifiedImages response;
		try{

			response = service.classify(options).execute();
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed classifying the image: " + VisualRequestObject.getName(), e);
			throw new MendixException(getExceptionString(e), e);
		}

		final List<IMendixObject> responseResults = new ArrayList<IMendixObject>();
		for(ClassifiedImage image : response.getImages()){

			for(ClassifierResult classifier : image.getClassifiers()){

				IMendixObject classifierObject;
				try {
					classifierObject = getClassifierEntity(context, classifier.getClassifierId());
				} catch (MendixException e) {
					LOGGER.error(e);

					if("default".equals(classifier.getName())){
						LOGGER.warn("You may have forgotten to create the default classifier");
					}

					throw new MendixException(e);
				}

				for(ClassResult visualClass : classifier.getClasses()){
					IMendixObject classifierClassObject = Core.instantiate(context, ClassifierClass.entityName);

					classifierClassObject.setValue(context, ClassifierClass.MemberNames.Name.toString(), visualClass.getClassName());
					classifierClassObject.setValue(context, ClassifierClass.MemberNames.Score.toString(), new BigDecimal(visualClass.getScore()));
					classifierClassObject.setValue(context, ClassifierClass.MemberNames.ClassifierClass_Classifier.toString(), classifierObject.getId());
					classifierClassObject.setValue(context, ClassifierClass.MemberNames.ClassifierClass_VisualRecognitionImage.toString(), VisualRequestObject.getMendixObject().getId());

					Core.commit(context, classifierClassObject);
				}

				responseResults.add(classifierObject);
			}
		}
		return responseResults;
	}
	
	public static String createClassifier(IContext context, String apikey, String url, Classifier classifier) throws CoreException, MendixException {
		LOGGER.debug("Executing CreateClassifier Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		final TrainingImagesZipFile negTrainingImagesZipFile = classifier.getClassifier_negativeTrainingImagesZipFile();
		final InputStream negTrainingImagesZipInputStream = negTrainingImagesZipFile != null ? new RestartableInputStream(context, negTrainingImagesZipFile.getMendixObject()) : null;

	    final CreateClassifierOptions options = new CreateClassifierOptions.Builder().
	    		name(classifier.getName())
	    		.positiveExamples(buildPositiveExamples(context, classifier.getClassifier_positiveTrainingImagesZipFiles()))
	    		.positiveExamplesFilename(buildPositiveExamplesFilenames(context, classifier.getClassifier_positiveTrainingImagesZipFiles()))
	    		.negativeExamples(negTrainingImagesZipInputStream)
	    		.negativeExamplesFilename(negTrainingImagesZipFile != null ? negTrainingImagesZipFile.getName() : null)
	    		.build();
	    
	    com.ibm.watson.developer_cloud.visual_recognition.v3.model.Classifier visualClassifier;
		try {
			visualClassifier = service.createClassifier(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed creating the classifier:"  +  classifier.getName(), e);
			throw new MendixException(getExceptionString(e), e);
		}

		return visualClassifier.getClassifierId();
	}

	public static List<IMendixObject> getClassifiers(IContext context, String apikey, String url) throws MendixException {
		LOGGER.debug("Executing getClassifiers Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		final ListClassifiersOptions options = new ListClassifiersOptions.Builder()
				.verbose(true)
				.build();

		final Classifiers classifiers;
		try {
			classifiers = service.listClassifiers(options).execute();
		} catch (Exception ex) {
			LOGGER.error("Watson Service connection - Failed to get the list of classifiers", ex);
			throw new MendixException(ex.getMessage(), ex);
		}

		return classifiers.getClassifiers().stream()
				.map(c -> createClassifierEntity(context, c))
				.collect(Collectors.toList());
	}

	public static void deleteClassifier(IContext context, String apikey, String url, String classifierId) throws MendixException {
		LOGGER.debug("Executing deleteClassifier Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		DeleteClassifierOptions options = new DeleteClassifierOptions.Builder()
				.classifierId(classifierId)
				.build();
		try {
			service.deleteClassifier(options).execute();
		} catch(Exception ex) {
			LOGGER.error("Watson Service connection - Failed to delete classifier: " + classifierId, ex);
			throw new MendixException(ex);
		}
	}

	public static List<IMendixObject> detectFaces(IContext context, String apikey, String url, Image image) throws MendixException {
		LOGGER.debug("Executing DetectFaces Connector...");
		

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		validateImageFile(context, image);

		final InputStream imageInputStream = new RestartableInputStream(context, image.getMendixObject());

		final DetectFacesOptions options = new DetectFacesOptions.Builder().
				imagesFile(imageInputStream).
				imagesFilename(image.getName()).
				build();
				
		DetectedFaces response;
		try {

			response = service.detectFaces(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed detecting the faces in the image: " + image.getName(), e);
			throw new MendixException(getExceptionString(e), e);
		}

		final List<IMendixObject> results = new ArrayList<IMendixObject>();
		for (ImageWithFaces imageFace : response.getImages()) {

			if(imageFace.getError() != null){
				LOGGER.warn("Error processing the image "+ imageFace.getImage() + ": " + imageFace.getError().getDescription());
				continue;
			}

			for(Face face :imageFace.getFaces()){

				IMendixObject faceObject = Core.instantiate(context, watsonservices.proxies.Face.entityName);

				if(face.getAge() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeMax.toString(), face.getAge().getMax());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeMin.toString(), face.getAge().getMin());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeScore.toString(), new BigDecimal(face.getAge().getScore()));
				}

				if(face.getGender() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.GenderName.toString(), face.getGender().getGender());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.GenderScore.toString(), new BigDecimal(face.getGender().getScore()));
				}

				if(face.getFaceLocation() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationHeight.toString(), face.getFaceLocation().getHeight());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationLeft.toString(), face.getFaceLocation().getLeft());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationTop.toString(), face.getFaceLocation().getTop());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationWidth.toString(), face.getFaceLocation().getWidth());
				}

				results.add(faceObject);
			}			
		}
		return results;
	}
	private static ClassifyOptions buildClassifyImagesOptions(List<Classifier> classifiers, InputStream imageToRecognizeInputStream, String imageToRecognizeFileName) throws MendixException {
		ClassifyOptions.Builder builder = new ClassifyOptions.Builder();
		if(!classifiers.isEmpty()) {
			List<String> classifierIds = new ArrayList<String>();

			for(Classifier classifier : classifiers){
				classifierIds.add(classifier.getClassifierId());
			}

			builder = builder.classifierIds(classifierIds);
		}

		return builder.imagesFile(imageToRecognizeInputStream)
				.imagesFilename(imageToRecognizeFileName)
				.build();
	}

	private static IMendixObject getClassifierEntity(IContext context,  String classifierId) throws MendixException{

		final List<IMendixObject> classifierObjectList = Core.retrieveXPathQueryEscaped(context, "//%s[%s ='%s']", CLASSIFIER_ENTITY_NAME, CLASSIFIER_ENTITY_PROPERTY, classifierId);

		if(classifierObjectList.isEmpty()){
			throw new MendixException("Not found a classifier object with id: " + classifierId);
		}

		return classifierObjectList.get(0);
	}

	private static IMendixObject createClassifierEntity(IContext context,
			com.ibm.watson.developer_cloud.visual_recognition.v3.model.Classifier classifier) {
		IMendixObject classifierObject = Core.instantiate(context, Classifier.entityName);
		classifierObject.setValue(context, Classifier.MemberNames.Name.toString(), classifier.getName());
		classifierObject.setValue(context, Classifier.MemberNames.ClassifierId.toString(), classifier.getClassifierId());
		classifierObject.setValue(context, Classifier.MemberNames.ClassifierOwner.toString(), classifier.getOwner());
		classifierObject.setValue(context, Classifier.MemberNames.Created.toString(), classifier.getCreated());
		classifierObject.setValue(context, Classifier.MemberNames.Status.toString(), classifier.getStatus());
		classifierObject.setValue(context, Classifier.MemberNames.Explanation.toString(), classifier.getExplanation());
		return classifierObject;
	}

	private static void validateImageFile(IContext context, Image image) throws MendixException {
		if (image == null || !image.getHasContents(context)) {
			LOGGER.error("Empty image provided");
			throw new MendixException("Image is empty");
		}
		final String imageFileName = new File(image.getName(context)).getName();

		// Get file extension
		final int extensionPos = imageFileName.lastIndexOf('.') + 1;
		final boolean haveExtension = extensionPos > 0 && extensionPos < imageFileName.length();
		final String imageFileExtension = haveExtension ? imageFileName.substring(extensionPos) : "";
		
		if(!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPG.equals(imageFileExtension.toLowerCase()) && 
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPEG.equals(imageFileExtension.toLowerCase()) && 
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_GIF.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_TIF.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_TIFF.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP.equals(imageFileExtension.toLowerCase())){
			
			final String errorMessage = "The input file doesn't have a valid extension (jpg, jpeg, png, gif, tif, tiff or zip) :" + image.getName();
			LOGGER.error(errorMessage);
			throw new MendixException(errorMessage);	
		}
	}

	private static Map<String, InputStream> buildPositiveExamples(IContext context, List<TrainingImagesZipFile> positiveExamples) {
		return positiveExamples.stream()
				.collect(Collectors.toMap(
						e -> e.getClassName() + "_positive_examples",
						e -> new RestartableInputStream(context, e.getMendixObject())
				));
	}

	private static Map<String, String> buildPositiveExamplesFilenames(IContext context, List<TrainingImagesZipFile> positiveExamples) {
		return positiveExamples.stream()
				.collect(Collectors.toMap(
						e -> e.getClassName() + "_positive_examples",
						e -> e.getName()
				));
	}

	private static String getExceptionString(Exception ex) {
		if (ex instanceof BadRequestException) {
			BadRequestException watsonException = (BadRequestException) ex;
			return watsonException.getResponse().message();
		}
		return ex.getMessage();
	}
}
