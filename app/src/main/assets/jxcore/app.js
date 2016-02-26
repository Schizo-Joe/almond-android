// -*- mode: js; indent-tabs-mode: nil; js-basic-offset: 4 -*-
//
// This file is part of ThingEngine
//
// Copyright 2015 Giovanni Campagna <gcampagn@cs.stanford.edu>
//
// See COPYING for details

console.log('ThingEngine-Android starting up...');

const Q = require('q');
const fs = require('fs');

const control = require('./control');
const Engine = require('thingengine-core');
const Tier = require('thingpedia').Tier;
const Frontend = require('./frontend/frontend');

const JavaAPI = require('./java_api');

function runEngine() {
    Q.longStackSupport = true;

    global.platform = require('./platform');

    platform.init().then(function() {
        console.log('Android platform initialized');
        console.log('Creating engine...');

        var engine = new Engine();
        var frontend = new Frontend();
        frontend.setEngine(engine);

        var engineRunning = false;
        var earlyStop = false;
        var controlChannel = new control.ControlChannel({
            // handle control methods here...

            foo: function(int) {
                console.log('Foo called on control channel with value ' + int);
                return int;
            },

            invokeCallback: function(callbackId, error, value) {
                JavaAPI.invokeCallback(callbackId, error, value);
            },

            stop: function() {
                if (engineRunning)
                    engine.stop();
                else
                    earlyStop = true;
                controlChannel.close();
            },

            setCloudId: function(cloudId, authToken) {
                if (engine.devices.hasDevice('thingengine-own-cloud'))
                    return false;
                if (!platform.setAuthToken(authToken))
                    return false;

                engine.devices.loadOneDevice({ kind: 'thingengine',
                                               tier: Tier.CLOUD,
                                               cloudId: cloudId,
                                               own: true }, true).done();
                return true;
            },

            setServerAddress: function(serverHost, serverPort, authToken) {
                if (engine.devices.hasDevice('thingengine-own-server'))
                    return false;
                if (authToken !== null) {
                    if (!platform.setAuthToken(authToken))
                        return false;
                }

                engine.devices.loadOneDevice({ kind: 'thingengine',
                                               tier: Tier.SERVER,
                                               host: serverHost,
                                               port: serverPort,
                                               own: true }, true).done();
                return true;
            },

            addApp: function(serializedApp, tier) {
                engine.apps.loadOneApp(serializedApp, {}, undefined, tier, null, null, true).done();
            },

            // For testing only!
            injectDevice: function(device) {
                console.log('Injecting device ' + JSON.stringify(device, 1));
                engine.devices.loadOneDevice(device, true).done();
            },

            createOmletFeedWithContact: function(contact) {
                return engine.messaging.getFeedWithContact(contact).then(function(feed) {
                    return feed.feedId;
                });
            },

            removeDevice: function(deviceId) {
                return engine.devices.removeDevice(engine.devices.getDevice(deviceId));
            },
        });

        return controlChannel.open().then(function() {
            // signal early to stop the engine
            // we don't need to async-wait for the result here, the call is sync
            // and execute on our thread
            JXMobile('controlReady').callNative();

            return Q.all([engine.open(), frontend.open()]);
        }).then(function() {
            engineRunning = true;
            if (earlyStop)
                return Q.all([engine.close(), frontend.close()]);
            return engine.run().finally(function() {
                return Q.all([engine.close(), frontend.close()]);
            });
        });
    }).catch(function(error) {
        console.log('Uncaught exception: ' + error.message);
        console.log(error.stack);
    }).finally(function() {
        console.log('Cleaning up');
        platform.exit();
    }).done();
}

console.log('Registering to JXMobile');
JXMobile('runEngine').registerToNative(runEngine);

