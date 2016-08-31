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

import org.apache.commons.codec.binary.Base64
import java.text.DecimalFormat
import groovy.transform.Field

@Field final USE_DEBUG = true
@Field final selectedCapabilities = [ "actuator", "sensor" ]
 
private getVendorName()       { "Pisanobot" }
private getVendorIcon()       { "http://i.imgur.com/BjTfDYk.png" }
private apiUrl()              { appSettings.apiUrl ?: "http://smartthings.pisano.org/api" }
private getVendorAuthPath()   { appSettings.vendorAuthPath ?: "http://smartthings.pisano.org/authorize" }
private getVendorTokenPath()  { appSettings.vendorTokenPath ?: "http://smartthings.pisano.org/access_token" }
 
definition(
    name: "Data Transport",
    namespace: "cpisano",
    author: "Christopher Pisano",
    description: "Post things to a WebService",
    category: "SmartThings Labs",
    iconUrl: "https://static-s.aa-cdn.net/img/ios/899550793/3c56aeea7cfdb1fe18eaad6a89ea8c40",
    iconX2Url: "https://static-s.aa-cdn.net/img/ios/899550793/3c56aeea7cfdb1fe18eaad6a89ea8c40",
    iconX3Url: "https://static-s.aa-cdn.net/img/ios/899550793/3c56aeea7cfdb1fe18eaad6a89ea8c40")

preferences(oauthPage: "deviceAuthorization") {
  page(name: "welcomePage")
  page(name: "subscribePage")
    page(name: "deviceAuthorization", title: "", nextPage: "devicesPage",
         install: false, uninstall: true) {
        section("Select Devices to Authorize") {
              for (capability in selectedCapabilities) {
                 input name: "${capability}Capability".toString(), type: "capability.$capability", title: "${capability.capitalize()} Things", multiple: true, required: false
              }
        }

    }    
  page(name: "devicesPage")
}

mappings {
  path("/message") {
    action: [ POST: "postMessage" ]
  }
  path("/app") {
    action: [ POST: "postApp" ]
  }
  path("/ping") {
    action: [ GET: "getPing" ]
  }
}

def getPing() {
  log.debug "ping pong"
    return ["pong"]
}

def welcomePage() {
//  cleanUpTokens()

  return dynamicPage(name: "welcomePage", nextPage: "subscribePage", uninstall: showUninstall) {
    section {
      paragraph title: "Welcome to the Octoblu SmartThings App!", "press 'Next' to continue"
    }
    if (state.installed) {
      section {
        input name: "showUninstall", type: "bool", title: "Uninstall", submitOnChange: true
        if (showUninstall) {
          state.removeDevices = removeDevices
          input name: "removeDevices", type: "bool", title: "Remove Octoblu devices", submitOnChange: true
          paragraph title: "Sorry to see you go!", "please email <support@octoblu.com> with any feedback or issues"
        }
      }
    }
  }
}

/*
    run a function at sunrise and sunset
*/
def checkSun() {
    // TODO: Use location information if zip is not provided
    def zip     = settings.zip as String
    def sunInfo = getSunriseAndSunset(zipCode: zip)
    def current = now()

    if(sunInfo.sunrise.time > current ||
        sunInfo.sunset.time  < current) {
        state.sunMode = "sunset"
    }
    else {
        state.sunMode = "sunrise"
    }

    log.info("Sunset: ${sunInfo.sunset.time}")
    log.info("Sunrise: ${sunInfo.sunrise.time}")
    log.info("Current: ${current}")
    log.info("sunMode: ${state.sunMode}")

    if(current < sunInfo.sunrise.time) {
        runIn(((sunInfo.sunrise.time - current) / 1000).toInteger(), setSunrise)
    }

    if(current < sunInfo.sunset.time) {
        runIn(((sunInfo.sunset.time - current) / 1000).toInteger(), setSunset)
    }
}

def subscribePage() {

createOAuthDevice() 

  return dynamicPage(name: "subscribePage", title: "Subscribe to SmartThing devices", nextPage: "deviceAuthorization") {
    section {
      // input name: "selectedCapabilities", type: "enum", title: "capability filter",
      // submitOnChange: true, multiple: true, required: false, options: [ "actuator", "sensor" ]
      for (capability in selectedCapabilities) {
         input name: "${capability}Capability".toString(), type: "capability.$capability", title: "${capability.capitalize()} Things", multiple: true, required: false
      }
    }
    section(" ") {
      input name: "pleaseCreateAppDevice", type: "bool", title: "Create a SmartApp device", defaultValue: true
      paragraph "A SmartApp device allows access to location and hub information for this installation"
    }
  }
}

def postMessage() {

    for (capability in selectedCapabilities) {      
      log.debug "capability ${capability}"
        
         def smartDevices = settings["${capability}Capability"]
         
         smartDevices.each { thing -> 
          log.debug("${thing.id} -- ${request.JSON.smartDeviceId}")
            
            if (!foundDevice && thing.id == request.JSON.smartDeviceId) {
            
            
              if (!request.JSON.command.startsWith("app-")) {
                def args = []
                if (request.JSON.args) {
                  request.JSON.args.each { k, v ->
                    args.push(v)
                  }
                }

                log.debug "command being sent: ${request.JSON.command}\targs to be sent: ${args}"
                thing."${request.JSON.command}"(*args)
              } else {
                log.debug "calling internal command ${request.JSON.command}"
                def commandData = [:]
                switch (request.JSON.command) {
                  case "app-get-value":
                    log.debug "got command value"
                    thing.supportedAttributes.each { attribute ->
                      commandData[attribute.name] = thing.latestValue(attribute.name)
                    }
                    break
                  case "app-get-state":
                    log.debug "got command state"
                    thing.supportedAttributes.each { attribute ->
                      commandData[attribute.name] = thing.latestState(attribute.name)?.value
                    }
                    break
                  case "app-get-device":
                    log.debug "got command device"
                    commandData = [
                      "id" : thing.id,
                      "displayName" : thing.displayName,
                      "name" : thing.name,
                      "label" : thing.label,
                      "capabilities" : thing.capabilities.collect{ thingCapability -> return thingCapability.name },
                      "supportedAttributes" : thing.supportedAttributes.collect{ attribute -> return attribute.name },
                      "supportedCommands" : thing.supportedCommands.collect{ command -> return ["name" : command.name, "arguments" : command.arguments ] }
                    ]
                    break
                  case "app-get-events":
                    log.debug "got command events"
                    commandData.events = []
                    thing.events().each { event ->
                      commandData.events.push(getEventData(event))
                    }
                    break
                  default:
                    commandData.error = "unknown command"
                    log.debug "unknown command ${request.JSON.command}"
                }

                commandData.command = request.JSON.command
                log.debug "with vendorDevice ${vendorDevice} for ${groovy.json.JsonOutput.toJson(commandData)}"

                def postParams = [
                  uri: apiUrl() + "messages",
                  headers: ["meshblu_auth_uuid": vendorDevice.uuid, "meshblu_auth_token": vendorDevice.token],
                  body: groovy.json.JsonOutput.toJson([ "devices" : [ "*" ], "payload" : commandData ])
                ]

                log.debug "posting params ${postParams}"

                try {
                  log.debug "calling httpPostJson!"
                  
                  post('/response', [ "devices" : [ "*" ], "payload" : commandData ]);
                  //httpPostJson(postParams) { response ->
                  //  debug "sent off command result"
                  //}
                } catch (e) {
                  log.error "unable to send command result ${e}"
                }


              }
             
            }
         }
        
    }

}

// --------------------------------------
def getDeviceInfo(device) {
  return [
    "id": device.id,
    "displayName": device.displayName,
  ]
}

def createDevices(smartDevices) {

  smartDevices.each { smartDevice ->
    def commands = [
      [ "name": "app-get-value" ],
      [ "name": "app-get-state" ],
      [ "name": "app-get-device" ],
      [ "name": "app-get-events" ]
    ]

    smartDevice.supportedCommands.each { command ->
      if (command.arguments.size()>0) {
        commands.push([ "name": command.name, "args": command.arguments ])
      } else {
        commands.push([ "name": command.name ])
      }
    }

    log.debug "creating device for ${smartDevice.id}"

    def schemas = [
      "version": "2.0.0",
      "message": [:]
    ]

    commands.each { command ->
      schemas."message"."$command.name" = [
        "type": "object",
        "properties": [
          "smartDeviceId": [
            "type": "string",
            "readOnly": true,
            "default": "$smartDevice.id",
            "x-schema-form": [
              "condition": "false"
            ]
          ],
          "command": [
            "type": "string",
            "readOnly": true,
            "default": "$command.name",
            "enum": ["$command.name"],
            "x-schema-form": [
              "condition": "false"
            ]
          ]
        ]
      ]

      if (command.args) {
        schemas."message"."$command.name"."properties"."args" = [
          "type": "object",
          "title": "Arguments",
          "properties": [:]
        ]

        command.args.each { arg ->
          def argLower = "$arg"
          argLower = argLower.toLowerCase()
          if (argLower == "color_map") {
            schemas."message"."$command.name"."properties"."args"."properties"."$argLower" = [
              "type": "object",
              "properties": [
                "hex": [
                  "type": "string"
                ],
                "level": [
                  "type": "number"
                ]
              ]
            ]
          } else {
            schemas."message"."$command.name"."properties"."args"."properties"."$argLower" = [
              "type": "$argLower"
            ]
          }
        }
      }
    }

    log.debug "UPDATED message schema: ${schemas}"

    def deviceProperties = [
      "schemas": schemas,
      "needsSetup": false,
      "online": true,
      "name": "${smartDevice.displayName}",
      "smartDeviceId": "${smartDevice.id}",
      "logo": "https://i.imgur.com/TsXefbK.png",
      "owner": "${state.vendorUuid}",
      "configureWhitelist": [],
      "discoverWhitelist": ["${state.vendorUuid}"],
      "receiveWhitelist": [],
      "sendWhitelist": [],
      "type": "device:${smartDevice.name.replaceAll('\\s','-').toLowerCase()}",
      "category": "smart-things",
      "meshblu": [
        "forwarders": [
          "received": [[
            "url": getApiServerUrl() + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/message",
            "method": "POST",
            "type": "webhook"
          ]]
        ]
      ]
    ]

   // updatePermissions(deviceProperties, smartDevice.id)
    def params = [
      uri: apiUrl() + "devices",
      //headers: ["Authorization": "Bearer ${state.vendorBearerToken}"],
      body: groovy.json.JsonOutput.toJson(deviceProperties)
    ]

    try {

        log.debug "creating new device for ${smartDevice.id} ${smartDevice.name}"
        httpPostJson(params) { response ->
          //state.vendorDevices[smartDevice.id] = getDeviceInfo(response.data)
        }


    } catch (e) {
      log.error "unable to create new device ${e}"
    }
  }
}


def devicesPage() {

log.debug "devices"

  state.vendorDevices = [:]



  def hasDevice = [:]
  hasDevice[app.id] = true
  selectedCapabilities.each { capability ->
    def smartDevices = settings["${capability}Capability"]
    createDevices(smartDevices)
    smartDevices.each { smartDevice ->
      hasDevice[smartDevice.id] = true
      //state.vendorDevices = [:]
      state.vendorDevices[smartDevice.id] = getDeviceInfo(smartDevice)
    }
  }

  /*
  log.debug "getting url ${postParams.uri}"
  try {
    httpGet(postParams) { response ->
      debug "devices json ${response.data.devices}"
      response.data.devices.each { device ->
        if (device.smartDeviceId && hasDevice[device.smartDeviceId]) {
          debug "found device ${device.uuid} with smartDeviceId ${device.smartDeviceId}"
          state.vendorDevices[device.smartDeviceId] = getDeviceInfo(device)
        }
        debug "has device: ${device.uuid} ${device.name} ${device.type}"
      }
    }
  } catch (e) {
    log.error "devices error ${e}"
  }
*/
 // selectedCapabilities.each { capability ->
 //   log.debug "checking devices for capability ${capability}"
    //createDevices(settings["${capability}Capability"])
  //}
  //if (pleaseCreateAppDevice)
   // createAppDevice()

  return dynamicPage(name: "devicesPage", title: "Smartapps Things", install: true) {
    section {
      paragraph title: "Please press 'Done' to finish setup", "and subscribe to SmartThing events"
     // paragraph title: "My Octoblu UUID:", "${state.vendorUuid}"
     // paragraph title: "My SmartThings in Octobu (${state.vendorDevices.size()}):", getDevInfo()
    }
  }
}

def createOAuthDevice() {

 def hub_id = null
 def name = null
 def type = null
 
 location.hubs.each { object ->
  hub_id = object.id
    name = object.name
    type = object.type
 };
 
  def oAuthDevice = [
    "name": "SmartThings",
    "owner": "4daabdf0-6b06-11e6-bdf4-0800200c9a66",
    "type": "device:register",
    "online": true,
    "hub": [
      "name": name,
      "type": type,
      "id": hub_id,
      "imageUrl": "https://i.imgur.com/TsXefbK.png",
      "callbackUrl": getApiServerUrl() 
    ]
  ]

  def postParams = [ uri: apiUrl()+"devices",
  body: groovy.json.JsonOutput.toJson(oAuthDevice)]

  try {
    httpPostJson(postParams) { response ->
     //log. debug "got new token for oAuth device ${response.data}"
     //state.hub = response.data.uuid
     // state.vendorOAuthToken = response.data.token
    }
  } catch (e) {
    log.error "unable to create oAuth device: ${e}"
  }

}

def getDeviceArray() {

}

def getDevices() {

    render contentType: "application/json", data: html, status: 200
}

def post(path, body) {
  
    def url = "http://smartthings.pisano.org/api/smartthings${path}"

    try {
        
        
        httpPostJson(uri: url ,  body: body) {response ->
            //log.debug "POSTED $path"
        }
    
    } catch (Exception e) {
        log.error "POSTED: $url $e"
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

def postEvent(name, device, value, description)
{
     def now = new Date();
     
       post('/event', [event: [
                 id: '',
                 date: now.format("yyyy-MM-dd'T'HH:mm:ss.S'Z'", TimeZone.getTimeZone('UTC')),
                 name: name,
                 deviceId: device,
                 value: value,
                 unit: '',
                 hub: '',
                 data: '',
                 zwave: '',
                 description: description,
                 descriptionText: ''
             ]]) 
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
    postEvent('temperature', '22af7a10-6a42-11e6-bdf4-0800200c9a66', value, json?.current_observation.temperature_string)
       
    
    log.debug value
    
    if (value > 0) {
      result = snowColor
    }

  if (value > 50) {
      result = clearColor
    }
    
    if (value > 70) {
      result = snowColor
    }


    if (value > 80) {
      result = snowColor
    }
    
    result = "#FFFA00";

    
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
    //def forecastColor = parseForecast(response)

    def temperature = response?.current_observation.temp_f;
    
    def value = temperature.toInteger()
    postEvent('temperature', '22af7a10-6a42-11e6-bdf4-0800200c9a66', value, response?.current_observation.temperature_string)

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
  state.installed = true;
  initialize()
}

def updated() {
  unsubscribe()
  log.debug "Updated with settings: ${settings}"
  def subscribed = [:]
  selectedCapabilities.each{ capability ->
      settings."${capability}Capability".each { thing ->
      if (subscribed[thing.id]) {
        return
      }
      subscribed[thing.id] = true
      thing.supportedAttributes.each { attribute ->
        log.debug "subscribe to attribute ${attribute.name}"
        subscribe thing, attribute.name, deviceEventHandler
      }
      thing.supportedCommands.each { command ->
        log.debug "subscribe to command ${command.name}"
        subscribeToCommand thing, command.name, deviceEventHandler
      }
      log.debug "subscribed to thing ${thing.id}"
    }
  }
  initialize()
}

def initialize() {
    weatherCheck()
   runEvery30Minutes(weatherCheck);
   
    
  def noParams = getSunriseAndSunset()
    log.debug "sunrise with no parameters: ${noParams.sunrise}"
  log.debug "sunset with no parameters: ${noParams.sunset}"   
}


def deviceEventHandler(evt) {
  log.debug "DISCRIPTION: ${evt.descriptionText}"
  log.debug "DATE         ${evt.isoDate}"
  log.debug "ID           ${evt.id}"    
  log.debug "EVENT - *************************************************************************"

  def deviceid = evt.deviceId

  if (evt.name == 'sunrise') {
    deviceid = 'e5415490-6ace-11e6-bdf4-0800200c9a66'
  }

  if (evt.name == 'sunset') {
    deviceid = 'f0beeb70-6ace-11e6-bdf4-0800200c9a66'        
  }

  post('/event', [event: [
      "date" : evt.isoDate,
      "id" : evt.id,
      "data" : evt.data,
      "description" : evt.description,
      "descriptionText" : evt.descriptionText,
      "displayName" : evt.displayName,
      "deviceId" : evt.deviceId,
      "hubId" : evt.hubId,
      "installedSmartAppId" : evt.installedSmartAppId,
      "isDigital" : evt.isDigital(),
      "isPhysical" : evt.isPhysical(),
      "isStateChange" : evt.isStateChange(),
      "locationId" : evt.locationId,
      "name" : evt.name,
      "source" : evt.source,
      "unit" : evt.unit,
      "value" : evt.value,
      "category" : "event",
      "type" : "device:smart-thing"
    ]])
}
