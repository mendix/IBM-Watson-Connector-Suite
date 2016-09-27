package watsonservices.utils;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifierOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectedFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageFace;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualRecognitionOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier.VisualClass;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;
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
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG = "png";
	private static final String WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP = "zip";
	private static final String DETECT_FACES_FILENAME = "VisualRecognition_DetectFaces_image.jpg";

	public static List<IMendixObject> classifyImage(IContext context, VisualRecognitionImage VisualRequestObject, List<Classifier> classifiers, String apikey) throws MendixException, CoreException {
		LOGGER.debug("Executing RecognizeImage Connector...");

		final VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey(apikey);

		final File imageToClassifyFile = new File(Core.getConfiguration().getTempPath() + VisualRequestObject.getName());	
		try(InputStream stream = Core.getFileDocumentContent(context, VisualRequestObject.getMendixObject())){

			Files.copy(stream, imageToClassifyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}catch(Exception e){
			LOGGER.error("There was a problem with the image file: " + imageToClassifyFile.getPath(), e);
			throw new MendixException(e);
		}

		final ClassifyImagesOptions options = buildClassifyImagesOptions(classifiers, imageToClassifyFile);
		VisualClassification response = null;
		try{

			response = service.classify(options).execute();
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed classifying the image: " + imageToClassifyFile.getName(), e);
			throw new MendixException(e);
		}finally{
			imageToClassifyFile.delete();
		}

		final List<IMendixObject> responseResults = new ArrayList<IMendixObject>();
		for(ImageClassification image : response.getImages()){

			for(VisualClassifier classifier : image.getClassifiers()){

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

				for(VisualClass visualClass : classifier.getClasses()){
					IMendixObject classifierClassObject = Core.instantiate(context, ClassifierClass.entityName);

					classifierClassObject.setValue(context, ClassifierClass.MemberNames.Name.toString(), visualClass.getName());
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
	
	public static String createClassifier(IContext context, Classifier classifier, String apikey) throws CoreException, MendixException {
		LOGGER.debug("Executing CreateClassifier Connector...");
		
		final VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey(apikey);

		final TrainingImagesZipFile posTrainingImagesZipFile = classifier.getClassifier_positiveTrainingImagesZipFile();
		final FileDocument posZipFileDocument = posTrainingImagesZipFile;
		final File posTempFile = new File(Core.getConfiguration().getTempPath() + posZipFileDocument.getName());

		final TrainingImagesZipFile negTrainingImagesZipFile = classifier.getClassifier_negativeTrainingImagesZipFile();
		final FileDocument negZipFileDocument = negTrainingImagesZipFile;
		final File negTempFile = new File(Core.getConfiguration().getTempPath() + negZipFileDocument.getName());

		try(InputStream postFileStream = Core.getFileDocumentContent(context, posTrainingImagesZipFile.getMendixObject()); 
			InputStream	negFileStream = Core.getFileDocumentContent(context, negTrainingImagesZipFile.getMendixObject())){

			Files.copy(postFileStream, posTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(negFileStream, negTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		}catch(Exception e){
			LOGGER.error("There was a problem with the ZIP files: " + posTempFile.getPath() + " and " + negTempFile.getPath(), e);
		}		

	    final ClassifierOptions options = new ClassifierOptions.Builder().
	    		classifierName(classifier.getName())
	    		.addClass(posTrainingImagesZipFile.getName(), posTempFile)
	    		.negativeExamples(negTempFile)
	    		.build();
	    
		VisualClassifier visualClassifier;
		try {
			visualClassifier = service.createClassifier(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed creating the classifier:"  +  classifier.getName(), e);
			throw new MendixException(e);
		}

		return visualClassifier.getId();
	}

	public static List<IMendixObject> detectFaces(IContext context, Image image, String apikey) throws MendixException {
		LOGGER.debug("Executing DetectFaces Connector...");
		
		final VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey(apikey);
			
		final File imageToDetectFaces = createImageFile(context, image);

		final VisualRecognitionOptions options = new VisualRecognitionOptions.Builder().
				images(imageToDetectFaces).
				build();
				
		DetectedFaces response;
		try {

			response = service.detectFaces(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed detecting the faces in the image: " + imageToDetectFaces.getName(), e);
			throw new MendixException(e);
		}
		finally{
			imageToDetectFaces.delete();
		}

		final List<IMendixObject> results = new ArrayList<IMendixObject>();
		for (ImageFace imageFace : response.getImages()) {

			if(imageFace.getError() != null){
				LOGGER.warn("Error processing the image "+ imageFace.getImage() + ": " + imageFace.getError().getDescription());
				continue;
			}

			for(Face face :imageFace.getFaces()){

				IMendixObject faceObject = Core.instantiate(context, watsonservices.proxies.Face.entityName);

				if(face.getAge() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeMax.toString(), face.getAge().getMax());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeMin.toString(), face.getAge().getMin());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.AgeScore.toString(), face.getAge().getScore().toString());
				}

				if(face.getGender() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.GenderName.toString(), face.getGender().getGender());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.GenderScore.toString(), face.getGender().getScore().toString());
				}

				if(face.getLocation() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationHeight.toString(), face.getLocation().getHeight());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationLeft.toString(), face.getLocation().getLeft());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationTop.toString(), face.getLocation().getTop());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.LocationWidth.toString(), face.getLocation().getWidth());
				}

				if(face.getIdentity() != null){
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.IdentityName.toString(), face.getIdentity().getName());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.IdentityScore.toString(), face.getIdentity().getScore().toString());
					faceObject.setValue(context, watsonservices.proxies.Face.MemberNames.TypeHierarchy.toString(), face.getIdentity().getTypeHierarchy());
				} 

				results.add(faceObject);
			}			
		}
		return results;
	}
	private static ClassifyImagesOptions buildClassifyImagesOptions(List<Classifier> classifiers, File imageToRecognizeFile){
		ClassifyImagesOptions options = null;

		if(!classifiers.isEmpty()) {
			List<String> classifierIds = new ArrayList<String>();

			for(Classifier classifier : classifiers){
				classifierIds.add(classifier.getName());
			}

			options = new ClassifyImagesOptions.Builder()
					.classifierIds(classifierIds)
					.images(imageToRecognizeFile)
					.build();
		}
		else {
			options = new ClassifyImagesOptions.Builder()
					.images(imageToRecognizeFile)
					.build();	
		}

		return options;
	}

	private static IMendixObject getClassifierEntity(IContext context,  String classifierName) throws MendixException{

		final List<IMendixObject> classifierObjectList = Core.retrieveXPathQueryEscaped(context, "//%s[%s ='%s']", CLASSIFIER_ENTITY_NAME, CLASSIFIER_ENTITY_PROPERTY, classifierName);

		if(classifierObjectList.isEmpty()){
			throw new MendixException("Not found a classifier object with id: " + classifierName);
		}

		return classifierObjectList.get(0);
	}

	private static File createImageFile(IContext context, Image image) throws MendixException {
		
		final String imageFileExtension = FilenameUtils.getExtension(image.getName());
		
		if(!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_JPG.equals(imageFileExtension.toLowerCase()) && 
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_PNG.equals(imageFileExtension.toLowerCase()) &&
				!WATSON_DETECT_FACES_SUPPORTED_IMAGE_EXTENSION_ZIP.equals(imageFileExtension.toLowerCase())){
			
			final String errorMessage = "The input file doesn't have a valid extension (jpg, png or zip) :" + image.getName();
			LOGGER.error(errorMessage);
			throw new MendixException(errorMessage);	
		}

		final File imageToDetectFaces = new File(Core.getConfiguration().getTempPath() + DETECT_FACES_FILENAME);
		
		try(final InputStream stream = Core.getFileDocumentContent(context, image.getMendixObject());) {

			Files.copy(stream, imageToDetectFaces.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			LOGGER.error("There was a problem with the image file: " + imageToDetectFaces.getPath(), e);
			throw new MendixException(e);
		}

		return imageToDetectFaces;
	}
}
