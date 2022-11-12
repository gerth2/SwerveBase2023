"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
/* eslint-disable camelcase */
var store_1 = require("@webbitjs/store");
var NT4_1 = require("./NT4");
var Nt4Provider = /** @class */ (function (_super) {
    __extends(Nt4Provider, _super);
    function Nt4Provider() {
        var _this = _super.call(this) || this;
        _this.connected = false;
        _this.topics = {};
        _this.client = _this.createClient('localhost');
        return _this;
    }
    Nt4Provider.prototype.connect = function (serverAddr) {
        this.client = this.createClient(serverAddr);
    };
    Nt4Provider.prototype.disconnect = function () {
        this.client.disconnect();
    };
    Nt4Provider.prototype.isConnected = function () {
        return this.connected;
    };
    Nt4Provider.prototype.userUpdate = function (key, value) {
        var topic = this.topics[key];
        if (topic) {
            this.client.publishNewTopic(topic.name, topic.type);
            this.client.addSample(topic.name, value);
            this.updateSource(topic.name, value);
        }
    };
    Nt4Provider.prototype.onTopicAnnounce = function (topic) {
        this.topics[topic.name] = topic;
    };
    Nt4Provider.prototype.onTopicUnannounce = function (topic) {
        delete this.topics[topic.name];
        this.removeSource(topic.name);
    };
    Nt4Provider.prototype.onNewTopicData = function (topic, _, value) {
        this.updateSource(topic.name, value);
    };
    Nt4Provider.prototype.onConnect = function () {
        this.connected = true;
    };
    Nt4Provider.prototype.onDisconnect = function () {
        this.connected = false;
    };
    Nt4Provider.prototype.createClient = function (serverAddr) {
        var _this = this;
        if (this.client) {
            this.client.disconnect();
            this.topics = {};
        }
        var appName = 'FRC Web Components';
        var client = new NT4_1.NT4_Client(serverAddr, appName, function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            return _this.onTopicAnnounce.apply(_this, args);
        }, function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            return _this.onTopicUnannounce.apply(_this, args);
        }, function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            return _this.onNewTopicData.apply(_this, args);
        }, function () { return _this.onConnect(); }, function () { return _this.onDisconnect(); });
        client.connect();
        client.subscribeAll(['/'], true);
        return client;
    };
    return Nt4Provider;
}(store_1.SourceProvider));
exports.default = Nt4Provider;
