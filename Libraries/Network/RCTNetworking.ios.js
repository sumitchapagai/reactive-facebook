/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow
 */

'use strict';

const NativeEventEmitter = require('../EventEmitter/NativeEventEmitter');
import NativeNetworking from './NativeNetworking';
const convertRequestBody = require('./convertRequestBody');

import type {RequestBody} from './convertRequestBody';

import type {NativeResponseType} from './XMLHttpRequest';

class RCTNetworking extends NativeEventEmitter {
  constructor() {
    super(NativeNetworking);
  }

  sendRequest(
    method: string,
    trackingName: string,
    url: string,
    headers: Object,
    data: RequestBody,
    responseType: NativeResponseType,
    incrementalUpdates: boolean,
    timeout: number,
    callback: (requestId: number) => mixed,
    withCredentials: boolean,
  ) {
    const body = convertRequestBody(data);
    NativeNetworking.sendRequest(
      {
        method,
        url,
        data: {...body, trackingName},
        headers,
        responseType,
        incrementalUpdates,
        timeout,
        withCredentials,
      },
      callback,
    );
  }

  abortRequest(requestId: number) {
    NativeNetworking.abortRequest(requestId);
  }

  clearCookies(callback: (result: boolean) => mixed) {
    NativeNetworking.clearCookies(callback);
  }
}

module.exports = new RCTNetworking();
