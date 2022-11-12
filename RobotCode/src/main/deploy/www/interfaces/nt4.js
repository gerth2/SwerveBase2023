"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NT4_Client = exports.NT4_Topic = void 0;
/* eslint-disable */
var msgpack_1 = require("./msgpack");
var typestrIdxLookup = {
    boolean: 0,
    double: 1,
    int: 2,
    float: 3,
    string: 4,
    json: 4,
    raw: 5,
    rpc: 5,
    msgpack: 5,
    protobuf: 5,
    'boolean[]': 16,
    'double[]': 17,
    'int[]': 18,
    'float[]': 19,
    'string[]': 20,
};
var NT4_Subscription = /** @class */ (function () {
    function NT4_Subscription() {
        this.uid = -1;
        this.topics = new Set();
        this.options = new NT4_SubscriptionOptions();
    }
    NT4_Subscription.prototype.toSubscribeObj = function () {
        return {
            topics: Array.from(this.topics),
            subuid: this.uid,
            options: this.options.toObj(),
        };
    };
    NT4_Subscription.prototype.toUnsubscribeObj = function () {
        return {
            subuid: this.uid,
        };
    };
    return NT4_Subscription;
}());
var NT4_SubscriptionOptions = /** @class */ (function () {
    function NT4_SubscriptionOptions() {
        this.periodic = 0.1;
        this.all = false;
        this.topicsOnly = false;
        this.prefix = false;
    }
    NT4_SubscriptionOptions.prototype.toObj = function () {
        return {
            periodic: this.periodic,
            all: this.all,
            topicsonly: this.topicsOnly,
            prefix: this.prefix,
        };
    };
    return NT4_SubscriptionOptions;
}());
var NT4_Topic = /** @class */ (function () {
    function NT4_Topic() {
        this.uid = -1; // "id" if server topic, "pubuid" if published
        this.name = '';
        this.type = '';
        this.properties = {};
    }
    NT4_Topic.prototype.toPublishObj = function () {
        return {
            name: this.name,
            type: this.type,
            pubuid: this.uid,
            properties: this.properties,
        };
    };
    NT4_Topic.prototype.toUnpublishObj = function () {
        return {
            pubuid: this.uid,
        };
    };
    NT4_Topic.prototype.getTypeIdx = function () {
        if (this.type in typestrIdxLookup) {
            return typestrIdxLookup[this.type];
        }
        else {
            return 5; // Default to binary
        }
    };
    return NT4_Topic;
}());
exports.NT4_Topic = NT4_Topic;
var NT4_Client = /** @class */ (function () {
    /**
     * Creates a new NT4 client without connecting.
     * @param serverAddr Network address of NT4 server
     * @param appName Identifier for this client (does not need to be unique).
     * @param onTopicAnnounce Gets called when server announces enough topics to form a new signal
     * @param onTopicUnannounce Gets called when server unannounces any part of a signal
     * @param onNewTopicData Gets called when any new data is available
     * @param onConnect Gets called once client completes initial handshake with server
     * @param onDisconnect Gets called once client detects server has disconnected
     */
    function NT4_Client(serverAddr, appName, onTopicAnnounce, onTopicUnannounce, onNewTopicData, onConnect, //
    onDisconnect) {
        var _this = this;
        this.ws = null;
        this.clientIdx = 0;
        this.useSecure = false;
        this.serverAddr = '';
        this.serverConnectionActive = false;
        this.serverConnectionRequested = false;
        this.serverTimeOffset_us = null;
        this.uidCounter = 0;
        this.subscriptions = new Map();
        this.publishedTopics = new Map();
        this.serverTopics = new Map();
        this.serverBaseAddr = serverAddr;
        this.appName = appName;
        this.onTopicAnnounce = onTopicAnnounce;
        this.onTopicUnannounce = onTopicUnannounce;
        this.onNewTopicData = onNewTopicData;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        setInterval(function () { return _this.ws_sendTimestamp(); }, 5000);
    }
    //////////////////////////////////////////////////////////////
    // PUBLIC API
    /** Starts the connection. The client will reconnect automatically when disconnected. */
    NT4_Client.prototype.connect = function () {
        if (!this.serverConnectionRequested) {
            this.serverConnectionRequested = true;
            this.ws_connect();
        }
    };
    /** Terminates the connection. */
    NT4_Client.prototype.disconnect = function () {
        if (this.serverConnectionRequested) {
            this.serverConnectionRequested = false;
            if (this.serverConnectionActive && this.ws) {
                this.ws.close();
            }
        }
    };
    /**
     * Add a new subscription, reading data at the specified frequency.
     * @param topicPatterns A list of topics or prefixes to include in the subscription.
     * @param prefixMode If true, use patterns as prefixes. If false, only subscribe to topics that are an exact match.
     * @param period The period to return data in seconds.
     * @returns A subscription ID that can be used to unsubscribe.
     */
    NT4_Client.prototype.subscribePeriodic = function (topicPatterns, prefixMode, period) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewUID();
        newSub.topics = new Set(topicPatterns);
        newSub.options.prefix = prefixMode;
        newSub.options.periodic = period;
        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub.uid;
    };
    /**
     * Add a new subscription, reading all value updates.
     * @param topicPatterns A list of topics or prefixes to include in the subscription.
     * @param prefixMode If true, use patterns as prefixes. If false, only subscribe to topics that are an exact match.
     * @returns A subscription ID that can be used to unsubscribe.
     */
    NT4_Client.prototype.subscribeAll = function (topicPatterns, prefixMode) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewUID();
        newSub.topics = new Set(topicPatterns);
        newSub.options.prefix = prefixMode;
        newSub.options.all = true;
        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub.uid;
    };
    /**
     * Add a new subscription, reading only topic announcements (not values).
     * @param topicPatterns A list of topics or prefixes to include in the subscription.
     * @param prefixMode If true, use patterns as prefixes. If false, only subscribe to topics that are an exact match.
     * @returns A subscription ID that can be used to unsubscribe.
     */
    NT4_Client.prototype.subscribeTopicsOnly = function (topicPatterns, prefixMode) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewUID();
        newSub.topics = new Set(topicPatterns);
        newSub.options.prefix = prefixMode;
        newSub.options.topicsOnly = true;
        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub.uid;
    };
    /** Given an existing subscription, unsubscribe from it. */
    NT4_Client.prototype.unsubscribe = function (subscriptionId) {
        var subscription = this.subscriptions.get(subscriptionId);
        if (!subscription) {
            throw 'Unknown subscription ID "' + subscriptionId + '"';
        }
        this.subscriptions.delete(subscriptionId);
        if (this.serverConnectionActive) {
            this.ws_unsubscribe(subscription);
        }
    };
    /** Unsubscribe from all current subscriptions. */
    NT4_Client.prototype.clearAllSubscriptions = function () {
        for (var _i = 0, _a = this.subscriptions.keys(); _i < _a.length; _i++) {
            var subscriptionId = _a[_i];
            this.unsubscribe(subscriptionId);
        }
    };
    /**
     * Set the properties of a particular topic.
     * @param topic The topic to update
     * @param properties The set of new properties
     */
    NT4_Client.prototype.setProperties = function (topic, properties) {
        // Update local topics
        var updateTopic = function (toUpdate) {
            for (var _i = 0, _a = Object.keys(properties); _i < _a.length; _i++) {
                var key = _a[_i];
                var value = properties[key];
                if (value === null) {
                    delete toUpdate.properties[key];
                }
                else {
                    toUpdate.properties[key] = value;
                }
            }
        };
        var publishedTopic = this.publishedTopics.get(topic);
        if (publishedTopic)
            updateTopic(publishedTopic);
        var serverTopic = this.serverTopics.get(topic);
        if (serverTopic)
            updateTopic(serverTopic);
        // Send new properties to server
        if (this.serverConnectionActive) {
            this.ws_setproperties(topic, properties);
        }
    };
    /** Set whether a topic is persistent.
     *
     * If true, the last set value will be periodically saved to
     * persistent storage on the server and be restored during server
     * startup. Topics with this property set to true will not be
     * deleted by the server when the last publisher stops publishing.
     */
    NT4_Client.prototype.setPersistent = function (topic, isPersistent) {
        this.setProperties(topic, { persistent: isPersistent });
    };
    /** Set whether a topic is retained.
     *
     * Topics with this property set to true will not be deleted by
     * the server when the last publisher stops publishing.
     */
    NT4_Client.prototype.setRetained = function (topic, isRetained) {
        this.setProperties(topic, { retained: isRetained });
    };
    /** Publish a new topic from this client with the provided name and type. */
    NT4_Client.prototype.publishNewTopic = function (topic, type) {
        if (this.publishedTopics.has(topic)) {
            return;
        }
        var newTopic = new NT4_Topic();
        newTopic.name = topic;
        newTopic.uid = this.getNewUID();
        newTopic.type = type;
        this.publishedTopics.set(topic, newTopic);
        if (this.serverConnectionActive) {
            this.ws_publish(newTopic);
        }
        return;
    };
    /** Unpublish a previously-published topic from this client. */
    NT4_Client.prototype.unpublishTopic = function (topic) {
        var topicObj = this.publishedTopics.get(topic);
        if (!topicObj) {
            throw 'Topic "' + topic + '" not found';
        }
        this.publishedTopics.delete(topic);
        if (this.serverConnectionActive) {
            this.ws_unpublish(topicObj);
        }
    };
    /** Send some new value to the server. The timestamp is whatever the current time is. */
    NT4_Client.prototype.addSample = function (topic, value) {
        var timestamp = this.getServerTime_us();
        if (timestamp === null)
            timestamp = 0;
        this.addTimestampedSample(topic, timestamp, value);
    };
    /** Send some new timestamped value to the server. */
    NT4_Client.prototype.addTimestampedSample = function (topic, timestamp, value) {
        var topicObj = this.publishedTopics.get(topic);
        if (!topicObj) {
            throw 'Topic "' + topic + '" not found';
        }
        var txData = (0, msgpack_1.serialize)([
            topicObj.uid,
            timestamp,
            topicObj.getTypeIdx(),
            value,
        ]);
        this.ws_sendBinary(txData);
    };
    //////////////////////////////////////////////////////////////
    // Server/Client Time Sync Handling
    /** Returns the current client time in microseconds. */
    NT4_Client.prototype.getClientTime_us = function () {
        return new Date().getTime() * 1000;
    };
    /** Returns the current server time in microseconds (or null if unknown). */
    NT4_Client.prototype.getServerTime_us = function () {
        if (this.serverTimeOffset_us === null) {
            return null;
        }
        else {
            return this.getClientTime_us() + this.serverTimeOffset_us;
        }
    };
    NT4_Client.prototype.ws_sendTimestamp = function () {
        var timeToSend = this.getClientTime_us();
        var txData = (0, msgpack_1.serialize)([-1, 0, typestrIdxLookup['int'], timeToSend]);
        this.ws_sendBinary(txData);
    };
    NT4_Client.prototype.ws_handleReceiveTimestamp = function (serverTimestamp, clientTimestamp) {
        var rxTime = this.getClientTime_us();
        // Recalculate server/client offset based on round trip time
        var rtt = rxTime - clientTimestamp;
        var serverTimeAtRx = serverTimestamp + rtt / 2.0;
        this.serverTimeOffset_us = serverTimeAtRx - rxTime;
        console.log('[NT4] New server time estimate: ' +
            (this.getServerTime_us() / 1000000.0).toString());
    };
    //////////////////////////////////////////////////////////////
    // Websocket Message Send Handlers
    NT4_Client.prototype.ws_subscribe = function (subscription) {
        this.ws_sendJSON('subscribe', subscription.toSubscribeObj());
    };
    NT4_Client.prototype.ws_unsubscribe = function (subscription) {
        this.ws_sendJSON('unsubscribe', subscription.toUnsubscribeObj());
    };
    NT4_Client.prototype.ws_publish = function (topic) {
        this.ws_sendJSON('publish', topic.toPublishObj());
    };
    NT4_Client.prototype.ws_unpublish = function (topic) {
        this.ws_sendJSON('unpublish', topic.toUnpublishObj());
    };
    NT4_Client.prototype.ws_setproperties = function (topic, newProperties) {
        this.ws_sendJSON('setproperties', {
            name: topic,
            update: newProperties,
        });
    };
    NT4_Client.prototype.ws_sendJSON = function (method, params) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify([
                {
                    method: method,
                    params: params,
                },
            ]));
        }
    };
    NT4_Client.prototype.ws_sendBinary = function (data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(data);
        }
    };
    //////////////////////////////////////////////////////////////
    // Websocket connection Maintenance
    NT4_Client.prototype.ws_onOpen = function () {
        // Set the flag allowing general server communication
        this.serverConnectionActive = true;
        console.log('[NT4] Connected with idx ' + this.clientIdx.toString());
        // Sync timestamps
        this.ws_sendTimestamp();
        // Publish any existing topics
        for (var _i = 0, _a = this.publishedTopics.values(); _i < _a.length; _i++) {
            var topic = _a[_i];
            this.ws_publish(topic);
        }
        // Subscribe to existing subscriptions
        for (var _b = 0, _c = this.subscriptions.values(); _b < _c.length; _b++) {
            var subscription = _c[_b];
            this.ws_subscribe(subscription);
        }
        // User connection-opened hook
        this.onConnect();
    };
    NT4_Client.prototype.ws_onClose = function (event) {
        var _this = this;
        // Clear flags to stop server communication
        this.ws = null;
        this.serverConnectionActive = false;
        // User connection-closed hook
        this.onDisconnect();
        // Clear out any local cache of server state
        this.serverTopics.clear();
        if (event.reason !== '') {
            console.log('[NT4] Socket is closed: ', event.reason);
        }
        if (!event.wasClean) {
            this.useSecure = !this.useSecure;
        }
        if (this.serverConnectionRequested) {
            setTimeout(function () { return _this.ws_connect(); }, 500);
        }
    };
    NT4_Client.prototype.ws_onError = function () {
        if (this.ws)
            this.ws.close();
    };
    NT4_Client.prototype.ws_onMessage = function (event) {
        var _this = this;
        if (typeof event.data === 'string') {
            // JSON array
            var msgData = JSON.parse(event.data);
            if (!Array.isArray(msgData)) {
                console.warn('[NT4] Ignoring text message, JSON parsing did not produce an array at the top level.');
                return;
            }
            msgData.forEach(function (msg) {
                // Validate proper format of message
                if (typeof msg !== 'object') {
                    console.warn('[NT4] Ignoring text message, JSON parsing did not produce an object.');
                    return;
                }
                if (!('method' in msg) || !('params' in msg)) {
                    console.warn('[NT4] Ignoring text message, JSON parsing did not find all required fields.');
                    return;
                }
                var method = msg['method'];
                var params = msg['params'];
                if (typeof method !== 'string') {
                    console.warn('[NT4] Ignoring text message, JSON parsing found "method", but it wasn\'t a string.');
                    return;
                }
                if (typeof params !== 'object') {
                    console.warn('[NT4] Ignoring text message, JSON parsing found "params", but it wasn\'t an object.');
                    return;
                }
                // Message validates reasonably, switch based on supported methods
                if (method === 'announce') {
                    var newTopic = new NT4_Topic();
                    newTopic.uid = params.id;
                    newTopic.name = params.name;
                    newTopic.type = params.type;
                    newTopic.properties = params.properties;
                    _this.serverTopics.set(newTopic.name, newTopic);
                    _this.onTopicAnnounce(newTopic);
                }
                else if (method === 'unannounce') {
                    var removedTopic = _this.serverTopics.get(params.name);
                    if (!removedTopic) {
                        console.warn('[NT4] Ignoring unannounce, topic was not previously announced.');
                        return;
                    }
                    _this.serverTopics.delete(removedTopic.name);
                    _this.onTopicUnannounce(removedTopic);
                }
                else if (method === 'properties') {
                    var topic = _this.serverTopics.get(params.name);
                    if (!topic) {
                        console.warn('[NT4] Ignoring set properties, topic was not previously announced.');
                        return;
                    }
                    for (var _i = 0, _a = Object.keys(params.update); _i < _a.length; _i++) {
                        var key = _a[_i];
                        var value = params.update[key];
                        if (value === null) {
                            delete topic.properties[key];
                        }
                        else {
                            topic.properties[key] = value;
                        }
                    }
                }
                else {
                    console.warn('[NT4] Ignoring text message - unknown method ' + method);
                    return;
                }
            });
        }
        else {
            // MSGPack
            (0, msgpack_1.deserialize)(event.data, { multiple: true }).forEach(function (unpackedData) {
                var topicID = unpackedData[0];
                var timestamp_us = unpackedData[1];
                // let typeIdx = unpackedData[2];
                var value = unpackedData[3];
                if (topicID >= 0) {
                    var topic = null;
                    for (var _i = 0, _a = _this.serverTopics.values(); _i < _a.length; _i++) {
                        var serverTopic = _a[_i];
                        if (serverTopic.uid === topicID) {
                            topic = serverTopic;
                            // return;
                        }
                    }
                    if (!topic) {
                        console.warn('[NT4] Ignoring binary data - unknown topic ID ' +
                            topicID.toString());
                        return;
                    }
                    _this.onNewTopicData(topic, timestamp_us, value);
                }
                else if (topicID === -1) {
                    _this.ws_handleReceiveTimestamp(timestamp_us, value);
                }
                else {
                    console.warn('[NT4] Ignoring binary data - invalid topic ID ' +
                        topicID.toString());
                }
            });
        }
    };
    NT4_Client.prototype.ws_connect = function () {
        var _this = this;
        this.clientIdx = Math.floor(Math.random() * 99999999);
        var port = 5810;
        var prefix = 'ws://';
        if (this.useSecure) {
            prefix = 'wss://';
            port = 5811;
        }
        this.serverAddr =
            prefix +
                this.serverBaseAddr +
                ':' +
                port.toString() +
                '/nt/' +
                this.appName +
                '_' +
                this.clientIdx.toString();
        this.ws = new WebSocket(this.serverAddr, 'networktables.first.wpi.edu');
        this.ws.binaryType = 'arraybuffer';
        this.ws.addEventListener('open', function () { return _this.ws_onOpen(); });
        this.ws.addEventListener('message', function (event) {
            return _this.ws_onMessage(event);
        });
        this.ws.addEventListener('close', function (event) {
            return _this.ws_onClose(event);
        });
        this.ws.addEventListener('error', function () { return _this.ws_onError(); });
    };
    //////////////////////////////////////////////////////////////
    // General utilties
    NT4_Client.prototype.getNewUID = function () {
        this.uidCounter++;
        return this.uidCounter + this.clientIdx;
    };
    return NT4_Client;
}());
exports.NT4_Client = NT4_Client;
