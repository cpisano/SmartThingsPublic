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
        paragraph "Version 0.3.0a"
    }
	// section("Battery") {
 //    	input "thebattery", "capability.battery", required: true, title: "Monitor Battery", multiple: true
 //  	}  
    // section("Energy Meter:") {
    //     input "energy_meter", "capability.energyMeter", required: true, title: "Energy Meter Devices", multiple: true
    // }
	section("Motion Sensor") {
    	input "montion_sensor", "capability.motionSensor", required: true, title: "Motion Sensor Device", multiple: true
  	}    
    // section("Power") {
    //     input "thepower", "capability.power", required: false, title: "Power Devices", multiple: true
    // }    
    // section("Power Meter:") {
    //     input "power_meter", "capability.powerMeter", required: true, title: "Power Meter Devices", multiple: true
    // }
    section("Power Outlets:") {
        input "powerstrip_meter", "capability.powerMeter", required: true, title: "Power Meter Devices", multiple: true
    }    
  	section("Smoke Detector") {
    	input "thesmoke", "capability.smokeDetector", title: "smoke", required: true, multiple: true
        //input "thecarbon", "capability.carbonMonoxideDetector", title: "carbon", required: true, multiple: true
  	}  
  	// section("Switch") {
   //  	input "myswitch", "capability.switch", title: "switch", required: true, multiple: true
  	// }    
    section("Door Sensors") {
        input "thecontact", "capability.contactSensor", title: "select the doors", required: true, multiple: true
    }     
    //section("Temperature Measurement:") {
    //    input "temperature", "capability.temperatureMeasurement", required: true, title: "Temperature Devices", multiple: true
   // }
	section("People") {
    	input "thepresence", "capability.presenceSensor", title: "presence", required: true, multiple: true
  	}    
    // section("Delay between check (default 1 minutes") {
    //     input "frequency", "number", title: "Number of minutes", description: "", required: false
    // }    
    // section("Via text message at this number (or via push notification if not specified") {
    //     input("recipients", "contact", title: "Send notifications to") {
    //         input "phone", "phone", title: "Phone number (optional)", required: false
    //     }
    // }    
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

def updateDeviceStatus() {
    thecontact.each { object ->
        //reportDevice('contact', object);
       // def currentTemperature = object.currentTemperature
    // def currPower = power_meter.currentValue("power")
    // def currEnergy = power_meter.currentValue("energy")       
        log.debug "${object.displayName}: ${object.currentTemperature}"

      post('/event', [event: [
                id: '',
                date: now.format("yyyy-MM-ddTHH:mm:ssZ", TimeZone.getTimeZone('UTC')),
                name: 'temperature',
                deviceId: object.id,
                value: object.currentTemperature,
                unit: '',
                hub: '',
                data: '',
                zwave: '',
                description: '${object.displayName} was ${object.currentTemperature}°F'
            ]])        

    }  

    montion_sensor.each { object ->
        //reportDevice('motion', object);
    }
    
    thepresence.each { object ->
        ///reportDevice('presence', object);
    }  

    powerstrip_meter.each { object ->
        //reportDevice('outlet', object)

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
}

def subscribeEvents() {
    

    subscribe(montion_sensor, "motion", deviceEventHandler)
    subscribe(montion_sensor, "temperature", deviceEventHandler)
    subscribe(montion_sensor, "battery", deviceEventHandler)

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
       
    registerDevices()
    subscribeEvents()

    runEvery5Minutes(registerDevices);
    runEvery5Minutes(updateDeviceStatus);

    sendMessage("starting")
    


    //def pollingInterval = 1
    //def ticklerSchedule = "0 0/${pollingInterval} * * * ?"    
    
    //schedule(ticklerSchedule, tickler)
}