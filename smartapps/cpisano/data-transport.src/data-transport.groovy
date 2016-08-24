/**
 *  Device Data Transport
 *
 *  Copyright 2016 Christopher Pisano
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Data Transport",
    namespace: "cpisano",
    author: "Christopher Pisano",
    description: "Post things to a WebService",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
   section("About") {
        paragraph "Please select the devices that should be under the watchful eye of {{ enter product name }}."
        paragraph "Version 0.4.2a"
    }
	section("Motion Sensor") {
    	input "montion_sensor", "capability.motionSensor", required: true, title: "Motion Sensor Device", multiple: true
  	}    
    section("Power Outlets:") {
        input "powerstrip_meter", "capability.powerMeter", required: true, title: "Power Meter Devices", multiple: true
    }    
  	section("Smoke Detector") {
    	input "thesmoke", "capability.smokeDetector", title: "smoke", required: true, multiple: true
        //input "thecarbon", "capability.carbonMonoxideDetector", title: "carbon", required: true, multiple: true
  	}  
  	section("Switch") {
     	input "myswitch", "capability.switch", title: "switch", required: true, multiple: true
  	 }    
    section("Door Sensors") {
        input "thecontact", "capability.contactSensor", title: "select the doors", required: true, multiple: true
    }     
	section("People") {
    	input "thepresence", "capability.presenceSensor", title: "presence", required: true, multiple: true
  	}    
	section("Color") {
    	input "color_control", "capability.colorControl", title: "presence", required: true, multiple: true
  	}    

}

def getDeviceArray() {

}

def getDevices() {

    render contentType: "application/json", data: html, status: 200
}

mappings {
  path("/devices") {
    action: [
      GET: "getDevices"
    ]
  }
}

def post(path, body) {
    try {
        
        httpPostJson(uri: "http://smartthings.pisano.org", path: path,  body: body) {response ->
            //log.debug "POSTED $path"
        }
    
    } catch (Exception e) {
        log.error "POSTED: $path $e"
    }      
}


def reportDevice(type, device) {

    log.debug "${type}: ${device.id} ${device.displayName}"

    post('/register', [device: [
            id: device.id,
            displayName: device.displayName,
            type: type
        ]])
    // try {
    
    //     httpPostJson(uri: "http://smartthings.pisano.org", path: '/register',  body: ) {response ->
    //         log.debug "${type}: ${device.id} ${device.displayName}"
    //     }
    
    // } catch (Exception e) {
    //     log.debug "${type}: ${device.id} ${device.displayName}"
    //     log.error "something went wrong: $e"
    // } 
}

def textContainsAnyOf(text, keywords)
{
	def result = '';
	for (int i = 0; i < keywords.size(); i++) {
		result = text.contains(keywords[i])
        if (result == true) return result
	}
    return result;
}

def parseForecast(json)
{

 
	def snowKeywords = ['snow','flurries','sleet']
    def rainKeywords = ['rain', 'showers', 'sprinkles', 'precipitation']
    def clearKeywords = ['clear']
    def sunnyKeywords = ['sunny']
    def hotKeywords = ['hot']
    def cloudyKeywords = ['overcast','cloudy']
    def result = '#000000';
    
    def rainColor = '#08088A';
    def snowColor = '#00CCFF';
    def clearColor = '#cccccc';
    def sunnyColor = '#FFFF00';
    def hotColor = '#FF3300';
    def cloudyColor = '#4C4C4C';
    
    
	def temperature = json?.current_observation.temp_f;
    
    def value = temperature.toInteger()
     def now = new Date();
     
       post('/event', [event: [
                 id: '',
                 date: now.format("yyyy-MM-dd'T'HH:mm:ss.S'Z'", TimeZone.getTimeZone('UTC')),
                 name: 'temperature',
                 deviceId: '22af7a10-6a42-11e6-bdf4-0800200c9a66',
                 value: value,
                 unit: '',
                 hub: '',
                 data: '',
                 zwave: '',
                 description: json?.current_observation.temperature_string
             ]])        
    
    log.debug value
    
    if (value > 0) {
    	result = snowColor
    }

	if (value > 50) {
    	result = clearColor
    }
    
    if (value > 70) {
    	result = sunnyColor
    }


    if (value > 80) {
    	result = hotColor
    }

    
    //.txt_forecast?.forecastday?.first()
    /*
	if (forecast) {
		def text = forecast?.fcttext?.toLowerCase()
        def day = forecast?.title
        
        log.debug text
		if (text) {
            if(textContainsAnyOf(text,cloudyKeywords)) result = cloudyColor
			if(textContainsAnyOf(text,clearKeywords)) result = clearColor
			if(textContainsAnyOf(text,sunnyKeywords)) result = sunnyColor
            if(textContainsAnyOf(text,hotKeywords)) result = hotColor
            if(textContainsAnyOf(text,rainKeywords)) result = rainColor
            if(textContainsAnyOf(text,snowKeywords)) result = snowColor
        }
        log.debug "Weather for $day : $text"
        log.debug "Setting weather lights to $result"
    }
    else
    {
    	 log.debug "Could not get weather!"
    }
    */
    log.debug result
    return result
}

def weatherCheck(evt) {

	def response = getWeatherFeature("conditions", "21212")
    def forecastColor = parseForecast(response)
    log.debug "setting color to $forecastColor"
   
    color_control.each { 
        it?.on()
    	it?.setColor(forecastColor) 
    }
}

def updateDeviceStatus() {
    thecontact.each { object ->
        //reportDevice('contact', object);
       // def currentTemperature = object.currentTemperature
    // def currPower = power_meter.currentValue("power")
    // def currEnergy = power_meter.currentValue("energy")       
  //       log.debug "${object.displayName}: ${object.currentTemperature}"

		// def now = new Date()

  //       def description = object.displayName + ' was ' + object.currentTemperature + '°F';

  //     post('/event', [event: [
  //               id: '',
  //               date: now.format("yyyy-MM-dd'T'HH:mm:ss.S'Z'", TimeZone.getTimeZone('UTC')),
  //               name: 'temperature',
  //               deviceId: object.id,
  //               value: object.currentTemperature,
  //               unit: '',
  //               hub: '',
  //               data: '',
  //               zwave: '',
  //               description: description
  //           ]])        

    }  

    montion_sensor.each { object ->
        //reportDevice('motion', object);
    }
    
    thepresence.each { object ->
      //  log.debug "${object.displayName}: ${object.currentPresence}"

      //   def now = new Date()

      //   def description = object.displayName + ' was ' + object.currentPresence + '°F';

      // post('/event', [event: [
      //           id: '',
      //           date: now.format("yyyy-MM-dd'T'HH:mm:ss.S'Z'", TimeZone.getTimeZone('UTC')),
      //           name: 'presence',
      //           deviceId: object.id,
      //           value: object.currentPresence,
      //           unit: '',
      //           hub: '',
      //           data: '',
      //           zwave: '',
      //           description: description
      //       ]]) 
    }  

    powerstrip_meter.each { object ->
      //  log.debug "${object.displayName}: ${object.Presence}"

      //   def now = new Date()

      //   def description = object.displayName + ' was ' + object.currentPresence + '°F';

      // post('/event', [event: [
      //           id: '',
      //           date: now.format("yyyy-MM-dd'T'HH:mm:ss.S'Z'", TimeZone.getTimeZone('UTC')),
      //           name: 'presence',
      //           deviceId: object.id,
      //           value: object.currentPresence,
      //           unit: '',
      //           hub: '',
      //           data: '',
      //           zwave: '',
      //           description: description
      //       ]]) 

    }
    
    thesmoke.each { object ->
        //reportDevice('smoke', object);
    }    
}

def registerDevices() {
    log.debug "apiServerUrl: ${getApiServerUrl()}"

    thecontact.each { object ->
        reportDevice('contact', object);
    }    
    
    montion_sensor.each { object ->
        reportDevice('motion', object);
    }
    
    thepresence.each { object ->
        reportDevice('presence', object);
    }    

    powerstrip_meter.each { object ->
        reportDevice('outlet', object)

    }
    
    thesmoke.each { object ->
        reportDevice('smoke', object);
    }
    
    myswitch.each { object ->
        reportDevice('switch', object);
    }    
}

def subscribeEvents() {
    

    subscribe(montion_sensor, "motion", deviceEventHandler)
    subscribe(montion_sensor, "temperature", deviceEventHandler)
    subscribe(montion_sensor, "battery", deviceEventHandler)
    
    subscribe(myswitch, "switch", deviceEventHandler)

    subscribe(powerstrip_meter, "energy", deviceEventHandler)    
    subscribe(powerstrip_meter, "power", deviceEventHandler)
    subscribe(powerstrip_meter, "switch", deviceEventHandler)

    subscribe(powerstrip_meter, "energy1", deviceEventHandler)    
    subscribe(powerstrip_meter, "power1", deviceEventHandler)
    subscribe(powerstrip_meter, "switch1", deviceEventHandler)

    subscribe(powerstrip_meter, "energy2", deviceEventHandler)    
    subscribe(powerstrip_meter, "power2", deviceEventHandler)
    subscribe(powerstrip_meter, "switch2", deviceEventHandler)

    subscribe(powerstrip_meter, "energy3", deviceEventHandler)    
    subscribe(powerstrip_meter, "power3", deviceEventHandler)
    subscribe(powerstrip_meter, "switch3", deviceEventHandler)

    subscribe(powerstrip_meter, "energy4", deviceEventHandler)    
    subscribe(powerstrip_meter, "power4", deviceEventHandler)
    subscribe(powerstrip_meter, "switch4", deviceEventHandler)

    subscribe(thecontact, "contact", deviceEventHandler)
    subscribe(thecontact, "temperature", deviceEventHandler)
    subscribe(thecontact, "battery", deviceEventHandler)
    
    subscribe(thepresence, "presence", deviceEventHandler)

    subscribe(thesmoke, "smoke", deviceEventHandler)
    subscribe(thesmoke, "battery", deviceEventHandler)
    
    subscribe(location, "sunset", deviceEventHandler)
    subscribe(location, "sunrise", deviceEventHandler)
    
    subscribe(location, "sunsetTime", deviceEventHandler)
    subscribe(location, "sunriseTime", deviceEventHandler)
    
    subscribe(location, "mode", deviceEventHandler)    
}

void sendMessage(msg)
{
    def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
    //def msg = "${contact.displayName} has been left open for ${minutes} minutes."
    log.info msg
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
            sendSms phone, msg
        } else {
            sendPush msg
        }
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"
    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def deviceEventHandler(evt) {
	log.debug "DISCRIPTION: ${evt.descriptionText}"
    log.debug "DATE        ${evt.isoDate}"
	log.debug "ID           ${evt.id}"    
    log.debug "EVENT - *************************************************************************"

  post('/event', [event: [
            id: evt.id,
            date: evt.isoDate,
            name: evt.name,
            deviceId: evt.deviceId,
            value: evt.stringValue,
            unit: evt.unit,
            hub: evt.hubId,
            data: evt.data,
            zwave: evt.description,
            description: evt.descriptionText
        ]])
   
}
 
def initialize() {
    
	def noParams = getSunriseAndSunset()
    log.debug "sunrise with no parameters: ${noParams.sunrise}"
	log.debug "sunset with no parameters: ${noParams.sunset}"
       
   // registerDevices()
   // subscribeEvents()

    runIn(1, registerDevices)
    runIn(1, subscribeEvents)
    weatherCheck()
   //	runEvery1Hour(updateDeviceStatus);
   runEvery1Hour(weatherCheck);

    //sendMessage("starting")
    


    //def pollingInterval = 1
    //def ticklerSchedule = "0 0/${pollingInterval} * * * ?"    
    
    //schedule(ticklerSchedule, tickler)
}