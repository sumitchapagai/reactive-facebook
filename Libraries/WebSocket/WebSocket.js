/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule WebSocket
 *
 */
'use strict';

var RCTDeviceEventEmitter = require('RCTDeviceEventEmitter');
var RCTWebSocketModule = require('NativeModules').WebSocketModule;

var Platform = require('Platform');

var WebSocketBase = require('WebSocketBase');

var Event = require('Event');
var MessageEvent = require('MessageEvent');

var WebSocketId = 0;

var CLOSE_NORMAL = 1000;

class WebSocket extends WebSocketBase {
  _socketId: number;
  _subs: any;

  connectToSocketImpl(url: string): void {
    this._socketId = WebSocketId++;

    RCTWebSocketModule.connect(url, this._socketId);

    this._registerEvents(this._socketId);
  }

  closeConnectionImpl(code?: number, reason?: string): void {
    this._closeWebSocket(this._socketId, code, reason);
  }

  cancelConnectionImpl(): void {
    this._closeWebSocket(this._socketId);
  }

  sendStringImpl(message: string): void {
    RCTWebSocketModule.send(message, this._socketId);
  }

  sendArrayBufferImpl(): void {
    // TODO
    console.warn('Sending ArrayBuffers is not yet supported');
  }

  _closeWebSocket(id: number, code?: number, reason?: string): void {
    if (Platform.OS === 'android') {
      /*
       * The status code 1000 means 'CLOSE_NORMAL'
       * Reason is empty string by to match browser behaviour
       * More info: https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent
       */
      var statusCode = typeof code === 'number' ? code : CLOSE_NORMAL;
      var closeReason = typeof reason === 'string' ? reason : '';

      RCTWebSocketModule.close(statusCode, closeReason, id);
    } else {
      RCTWebSocketModule.close(id);
    }
  }

  _unregisterEvents(): void {
    this._subs.forEach(e => e.remove());
    this._subs = [];
  }

  _registerEvents(id: number): void {
    this._subs = [
      RCTDeviceEventEmitter.addListener('websocketMessage', ev => {
        if (ev.id !== id) {
          return;
        }
        var event = new MessageEvent('message', {
          data: ev.data
        });
        this.onmessage && this.onmessage(event);
        this.dispatchEvent(event);
      }),
      RCTDeviceEventEmitter.addListener('websocketOpen', ev => {
        if (ev.id !== id) {
          return;
        }
        this.readyState = this.OPEN;
        var event = new Event('open');
        this.onopen && this.onopen(event);
        this.dispatchEvent(event);
      }),
      RCTDeviceEventEmitter.addListener('websocketClosed', ev => {
        if (ev.id !== id) {
          return;
        }
        this.readyState = this.CLOSED;
        var event = new Event('close');
        event.code = ev.code;
        event.reason = ev.reason;
        this.onclose && this.onclose(event);
        this.dispatchEvent(event);
        this._unregisterEvents();
        this._closeWebSocket(id);
      }),
      RCTDeviceEventEmitter.addListener('websocketFailed', ev => {
        if (ev.id !== id) {
          return;
        }
        var event = new Event('error');
        event.message = ev.message;
        this.onerror && this.onerror(event);
        this.dispatchEvent(event);
        this._unregisterEvents();
        this._closeWebSocket(id);
      })
    ];
  }
}

module.exports = WebSocket;
