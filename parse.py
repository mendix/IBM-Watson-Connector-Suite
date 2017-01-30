#!/usr/bin/env python
import json
import os

# vcap_services_data = os.environ.get('VCAP_SERVICES')
vcap_services_data = open("bluemix_vcap.json").read()

data = json.loads(vcap_services_data)

data['VCAP_SERVICES']['alchemy_api']
data['VCAP_SERVICES']['conversation']
data['VCAP_SERVICES']['language_translator']
data['VCAP_SERVICES']['text_to_speech']
data['VCAP_SERVICES']['watson_vision_combined']
