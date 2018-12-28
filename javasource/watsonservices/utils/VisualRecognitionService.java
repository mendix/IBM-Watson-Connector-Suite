package watsonservices.utils;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImage;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifierResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.CreateClassifierOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectFacesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectedFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageWithFaces;
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
	private static final String CLASSIFIER_ENTITY_PROPERTY = Classifier.MemberNames.Name.name();
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPG = "jpg";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPEG = "jpeg";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG = "png";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP = "zip";
	private static final String WATSON_VISUAL_RECOGNITION_VERSION_DATE = "2018-03-19";

	public static List<IMendixObject> classifyImage(IContext context, VisualRecognitionImage VisualRequestObject, List<Classifier> classifiers, String apikey, String url) throws MendixException {
		LOGGER.debug("Executing RecognizeImage Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		final InputStream imageInputStream = new RestartableInputStream(context, VisualRequestObject.getMendixObject());

		final ClassifyOptions options = buildClassifyImagesOptions(classifiers, imageInputStream, VisualRequestObject.getName());
		final ClassifiedImages response;
		try{

			response = service.classify(options).execute();
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed classifying the image: " + VisualRequestObject.getName(), e);
			throw new MendixException(e);
		}

		final List<IMendixObject> responseResults = new ArrayList<IMendixObject>();
		for(ClassifiedImage image : response.getImages()){

			for(ClassifierResult classifier : image.getClassifiers()){

				IMendixObject classifierObject;
				try {
					classifierObject = getClassifierEntity(context, classifier.getName());
				} catch (MendixException e) {
					LOGGER.error(e);

					if("default".equals(classifier.getName())){
						LOGGER.warn("You may have forgotten to create the default classifier on the app startup microflow");
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
	
	public static String createClassifier(IContext context, Classifier classifier, String apikey, String url) throws CoreException, MendixException {
		LOGGER.debug("Executing CreateClassifier Connector...");

		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final VisualRecognition service = new VisualRecognition(WATSON_VISUAL_RECOGNITION_VERSION_DATE, iamOptions);
		service.setApiKey(apikey);
		service.setEndPoint(url);

		final TrainingImagesZipFile posTrainingImagesZipFile = classifier.getClassifier_positiveTrainingImagesZipFile();
		final InputStream posTrainingImagesZipInputStream = new RestartableInputStream(context, posTrainingImagesZipFile.getMendixObject());
		final TrainingImagesZipFile negTrainingImagesZipFile = classifier.getClassifier_negativeTrainingImagesZipFile();
		final InputStream negTrainingImagesZipInputStream = new RestartableInputStream(context, negTrainingImagesZipFile.getMendixObject());

		final String positiveExamplesClass = classifier.getName() + "_positive_examples";

	    final CreateClassifierOptions options = new CreateClassifierOptions.Builder().
	    		name(classifier.getName())
	    		.addPositiveExamples(positiveExamplesClass, posTrainingImagesZipInputStream)
	    		.positiveExamplesFilename(Collections.singletonMap(positiveExamplesClass, posTrainingImagesZipFile.getName()))
	    		.negativeExamples(negTrainingImagesZipInputStream)
	    		.negativeExamplesFilename(negTrainingImagesZipFile.getName())
	    		.build();
	    
	    com.ibm.watson.developer_cloud.visual_recognition.v3.model.Classifier visualClassifier;
		try {
			visualClassifier = service.createClassifier(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed creating the classifier:"  +  classifier.getName(), e);
			throw new MendixException(e);
		}

		return visualClassifier.getClassifierId();
	}

	public static List<IMendixObject> detectFaces(IContext context, Image image, String apikey, String url) throws MendixException {
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
			throw new MendixException(e);
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

	private static IMendixObject getClassifierEntity(IContext context,  String classifierName) throws MendixException{

		final List<IMendixObject> classifierObjectList = Core.retrieveXPathQueryEscaped(context, "//%s[%s ='%s']", CLASSIFIER_ENTITY_NAME, CLASSIFIER_ENTITY_PROPERTY, classifierName);

		if(classifierObjectList.isEmpty()){
			throw new MendixException("Not found a classifier object with id: " + classifierName);
		}

		return classifierObjectList.get(0);
	}

	private static void validateImageFile(IContext context, Image image) throws MendixException {
		final String imageFileName = new File(image.getName(context)).getName();

		// Get file extension
		final int extensionPos = imageFileName.lastIndexOf('.') + 1;
		final boolean haveExtension = extensionPos > 0 && extensionPos < imageFileName.length();
		final String imageFileExtension = haveExtension ? imageFileName.substring(extensionPos) : "";
		
		if(!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPG.equals(imageFileExtension.toLowerCase()) && 
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPEG.equals(imageFileExtension.toLowerCase()) && 
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP.equals(imageFileExtension.toLowerCase())){
			
			final String errorMessage = "The input file doesn't have a valid extension (jpg, jpeg, png or zip) :" + image.getName();
			LOGGER.error(errorMessage);
			throw new MendixException(errorMessage);	
		}
	}
}
