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
        paragraph "Version 0.1.1a"
    }
	section("Battery") {
    	input "thebattery", "capability.battery", required: true, title: "Monitor Battery", multiple: true
  	}  
    section("Energy Meter:") {
        input "energy_meter", "capability.energyMeter", required: true, title: "Energy Meter Devices", multiple: true
    }
	section("Motion Sensor") {
    	input "montion_sensor", "capability.motionSensor", required: true, title: "Motion Sensor Device", multiple: true
  	}    
    section("Power") {
        input "thepower", "capability.power", required: false, title: "Power Devices", multiple: true
    }    
    section("Power Meter:") {
        input "power_meter", "capability.powerMeter", required: true, title: "Power Meter Devices", multiple: true
    }
  	section("Smoke Detector") {
    	input "thesmoke", "capability.smokeDetector", title: "smoke", required: true, multiple: true
        input "thecarbon", "capability.carbonMonoxideDetector", title: "carbon", required: true, multiple: true
  	}  
  	section("Switch") {
    	input "myswitch", "capability.switch", title: "switch", required: true, multiple: true
  	}     
    section("Temperature Measurement:") {
        input "temperature", "capability.temperatureMeasurement", required: true, title: "Temperature Devices", multiple: true
    }
}

/**
 *	Scheduled event handler
 *  
 *	Called at the specified interval to poll the metering switch.
 *	This keeps the device active otherwise the power events do not get sent
 *
 *	evt		The scheduler event (always null)
 */
def tickler(evt) {
    //power_meter.ping()

    def currPower = power_meter.currentValue("power")
    def currEnergy = power_meter.currentValue("energy")
    
    log.debug "-meterHandler display name: power meterValue: ${currPower}W   ${currEnergy}"
    
    def params = [
        uri: "http://smartthings.pisano.org:81",
        path: "/test"
    ]
/**
    try {
        httpGet(params) { resp ->
            resp.headers.each {
               log.debug "${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }   
    */
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
	log.debug "DATA:        ${evt.data}"
    log.debug "SOURCE       ${evt.source}"    
    log.debug "HUB          ${evt.hubId}"
    log.debug "VALUE        ${evt.stringValue} ${evt.unit}"
    log.debug "             ${evt.deviceId}"
    log.debug "DEVICE       ${evt.device}"
    log.debug "             ${evt.displayName}"
    log.debug "NAME         ${evt.name}"
	log.debug "             ${evt.descriptionText}"
	log.debug "DISCRIPTION: ${evt.description}"
    log.debug "DATE?        ${evt.dateValue instanceof Date} ${evt.isoDate}"
	log.debug "ID           ${evt.id}"    
    log.debug "EVENT - *************************************************************************"

    try {
        
        httpPostJson(uri: "http://smartthings.pisano.org", path: '/event',  body: [event: [
        	id: evt.id,
            date: evt.isoDate,
            name: evt.name,
            displayName: evt.displayName,
            deviceId: evt.deviceId,
            value: evt.stringValue,
            unit: evt.unit,
            hub: evt.hubId,
            source: evt.source,
            data: evt.data,
        ]]) {response ->
			log.debug "POSTED"
		}
    
    } catch (Exception e) {
        log.error "something went wrong: $e"
    }    
   
}

def reportDevice(type, device) {
    try {
    
        httpPostJson(uri: "http://smartthings.pisano.org", path: '/register',  body: [device: [
        	id: device.id,
            displayName: device.displayName,
            type: type
        ]]) {response ->
			log.debug "${type}: ${device.id} ${device.displayName}"
		}
    
    } catch (Exception e) {
    	log.debug "${type}: ${device.id} ${device.displayName}"
        log.error "something went wrong: $e"
    } 
}

def initialize() {
    
	def noParams = getSunriseAndSunset()
    log.debug "sunrise with no parameters: ${noParams.sunrise}"
	log.debug "sunset with no parameters: ${noParams.sunset}"
    
    thebattery.each { object ->
    	reportDevice('battery', object);
    }

    energy_meter.each { object ->
    	reportDevice('energy', object);
    }
    
    montion_sensor.each { object ->
    	reportDevice('motion', object);
    }
    
    thepower.each { object ->
    	reportDevice('powerSource', object);
    }    
    
    power_meter.each { object ->
    	reportDevice('power', object);
    }
    
    thesmoke.each { object ->
    	reportDevice('smoke', object);
    }
    
    thecarbon.each { object ->
    	reportDevice('carbonMonoxide', object);
    }  
    
    myswitch.each { object ->
    	reportDevice('switch', object);
    }
    
    temperature.each { object ->
    	reportDevice('temperature', object);
    }     
    
    subscribe(thebattery, "battery", deviceEventHandler)
    subscribe(energy_meter, "energy", deviceEventHandler)
    subscribe(montion_sensor, "motion", deviceEventHandler)
    subscribe(thepower, "powerSource", deviceEventHandler)
    subscribe(power_meter, "power", deviceEventHandler)
    subscribe(thesmoke, "smoke", deviceEventHandler)
    subscribe(thecarbon, "carbonMonoxide", deviceEventHandler)
    subscribe(myswitch, "switch", deviceEventHandler)
    subscribe(temperature, "temperature", deviceEventHandler)
    
	subscribe(location, "sunset", deviceEventHandler)
	subscribe(location, "sunrise", deviceEventHandler)
    
    subscribe(location, "sunsetTime", deviceEventHandler)
	subscribe(location, "sunriseTime", deviceEventHandler)
    
    subscribe(location, "mode", deviceEventHandler)

    //def pollingInterval = 1
    //def ticklerSchedule = "0 0/${pollingInterval} * * * ?"    
    
    //schedule(ticklerSchedule, tickler)
}