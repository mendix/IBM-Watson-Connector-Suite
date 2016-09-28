# IBM Watson Connector Kit

Collection of connectors which brings the [IBM Watson Cognitive services](https://www.ibm.com/watson/developercloud/) to the Mendix Platform.

## Getting Started

Check out this project and you will ready to go using the [Mendix Modeler](https://appstore.home.mendix.com/index3.html) to deploy locally or in the Mendix free tier, because it is a example app plus connectors pointing to several IBM Watson Services.

### Prerequisities

- Mendix User account, [sign up here](https://www.mendix.com/try-now/)
- IBM Bluemix account, [sign up here](https://console.ng.bluemix.net/registration/)
- Mendix Modeler (only Windows compatible), [download here](https://appstore.home.mendix.com/index3.html)

### Features

These are the Watson APIs which have a connector implemented:
- Alchemy Language - [Keywords](https://www.ibm.com/watson/developercloud/alchemy-language/api/v1/#keywords)
- Conversation - [Send Message](http://www.ibm.com/watson/developercloud/conversation/api/v1/#send_message)
- Language Translation - [Translate](http://www.ibm.com/watson/developercloud/language-translation/api/v2/#translate)
- Language Translation - [Get Identifiable Languages](http://www.ibm.com/watson/developercloud/language-translation/api/v2/#identifiable_languages)
- Text to Speech - [Synthesize](http://www.ibm.com/watson/developercloud/text-to-speech/api/v1/#synthesize audio)
- Visual Recognition - [Classify Image](http://www.ibm.com/watson/developercloud/visual-recognition/api/v3/#classify_an_image)
- Visual Recognition - [Create Classifier](http://www.ibm.com/watson/developercloud/visual-recognition/api/v3/#create_a_classifier)
- Visual Recognition - [Detect Faces](http://www.ibm.com/watson/developercloud/visual-recognition/api/v3/#detect_faces)

### Installing

The project includes two modules that represents the list of available connectors and examples respectively. Both modules are structured following the same categories used by IBM in its portfolio.

![Project module structure words](documentation/images/documentation_image_project_module_structure.png)

To be able to use the Watson Connector Kit in your project, you will have to export the WatsonServices module and import it in the project you may want to it.

![EXport module](documentation/images/documentation_image_export_module.png)

Once you have imported the module in your project, you will have at your disposal the collection of new IBM Watson connectors to use in any microflow.

![Connectors available](documentation/images/documentation_image_connectors_available.png)

### Configuration

In your IBM Bluemix console, every instance of a Watson service will have a section called ""Service credentials" which will provide you the data in a format like following:
```
{
  "credentials": {
    "url": "<SERVICE_URL_PROVIDED_BY_IBM_BLUEMIX>",
    "note": "It may take up to 5 minutes for this key to become active",
    "apikey": "<YOUR_API_KEY>"
  }
}
```

Or if the service does not require apikey, it will require instead username and password

```
{
  "credentials": {
    "url": "<SERVICE_URL_PROVIDED_BY_IBM_BLUEMIX>",
    "password": "<YOUR_PASSWORD>",
    "username": "<YOUR_USERNAME>"
  }
}
```
Please following the instructions in the Example app to provide the service credentials accordingly in the app.


## Test environments

The Example app has been tested locally and in a sandbox. It has not been tested in IBM Bluemix yet, although the model provides the configuration files. 

## Known issues

The ChatWidget doesn't show the username properly when the Example App is deployed in a sandbox

## Built With

* Mendix Modeler 6.6.0
* Eclipse IDE Neon

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/mendix/IBM-Watson-Connector-Kit/tags).

## License

This project is licensed under the Apache License v2 - see the [LICENSE](LICENSE-2.0.txt) file for details
