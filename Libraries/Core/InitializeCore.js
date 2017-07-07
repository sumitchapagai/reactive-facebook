/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule InitializeCore
 * @flow
 */

/* eslint-disable strict */
/* globals window: true */

/**
 * Sets up global variables typical in most JavaScript environments.
 *
 *   1. Global timers (via `setTimeout` etc).
 *   2. Global console object.
 *   3. Hooks for printing stack traces with source maps.
 *
 * Leaves enough room in the environment for implementing your own:
 *
 *   1. Require system.
 *   2. Bridged modules.
 *
 */
'use strict';

if (global.GLOBAL === undefined) {
  global.GLOBAL = global;
}

if (global.window === undefined) {
  global.window = global;
}

const defineLazyObjectProperty = require('defineLazyObjectProperty');

/**
 * Sets an object's property. If a property with the same name exists, this will
 * replace it but maintain its descriptor configuration. By default, the property
 * will replaced with a lazy getter.
 *
 * The original property value will be preserved as `original[PropertyName]` so
 * that, if necessary, it can be restored. For example, if you want to route
 * network requests through DevTools (to trace them):
 *
 *   global.XMLHttpRequest = global.originalXMLHttpRequest;
 *
 * @see https://github.com/facebook/react-native/issues/934
 */
function defineProperty<T>(
  object: Object,
  name: string,
  getValue: () => T,
  eager?: boolean
): void {
  const descriptor = Object.getOwnPropertyDescriptor(object, name);
  if (descriptor) {
    const backupName = `original${name[0].toUpperCase()}${name.substr(1)}`;
    Object.defineProperty(object, backupName, {
      ...descriptor,
      value: object[name],
    });
  }

  const {enumerable, writable, configurable} = descriptor || {};
  if (descriptor && !configurable) {
    console.error('Failed to set polyfill. ' + name + ' is not configurable.');
    return;
  }

  if (eager === true) {
    Object.defineProperty(object, name, {
      configurable: true,
      enumerable: enumerable !== false,
      writable: writable !== false,
      value: getValue(),
    });
  } else {
    defineLazyObjectProperty(object, name, {
      get: getValue,
      enumerable: enumerable !== false,
      writable: writable !== false,
    });
  }
}

// Set up process
global.process = global.process || {};
global.process.env = global.process.env || {};
if (!global.process.env.NODE_ENV) {
  global.process.env.NODE_ENV = __DEV__ ? 'development' : 'production';
}

// Setup the Systrace profiling hooks if necessary
if (global.__RCTProfileIsProfiling) {
  const Systrace = require('Systrace');
  Systrace.setEnabled(true);
}

if (__DEV__ && global.performance === undefined) {
  const Systrace = require('Systrace');
  global.performance = Systrace.getUserTimingPolyfill();
}

// Set up console
const ExceptionsManager = require('ExceptionsManager');
ExceptionsManager.installConsoleErrorReporter();

// Set up error handler
if (!global.__fbDisableExceptionsManager) {
  const handleError = (e, isFatal) => {
    try {
      ExceptionsManager.handleException(e, isFatal);
    } catch (ee) {
      /* eslint-disable no-console-disallow */
      console.log('Failed to print error: ', ee.message);
      /* eslint-enable no-console-disallow */
      throw e;
    }
  };

  const ErrorUtils = require('ErrorUtils');
  ErrorUtils.setGlobalHandler(handleError);
}

// Set up collections
// We can't make these lazy because `Map` checks for `global.Map` (which wouldc
// not exist if it were lazily defined).
defineProperty(global, 'Map', () => require('Map'), true);
defineProperty(global, 'Set', () => require('Set'), true);

// Set up Promise
// The native Promise implementation throws the following error:
// ERROR: Event loop not supported.
defineProperty(global, 'Promise', () => require('Promise'));

// Set up timers
const defineLazyTimer = name => {
  defineProperty(global, name, () => require('JSTimers')[name]);
};
defineLazyTimer('setTimeout');
defineLazyTimer('setInterval');
defineLazyTimer('setImmediate');
defineLazyTimer('clearTimeout');
defineLazyTimer('clearInterval');
defineLazyTimer('clearImmediate');
defineLazyTimer('requestAnimationFrame');
defineLazyTimer('cancelAnimationFrame');
defineLazyTimer('requestIdleCallback');
defineLazyTimer('cancelIdleCallback');

// Set up XHR
// The native XMLHttpRequest in Chrome dev tools is CORS aware and won't
// let you fetch anything from the internet
defineProperty(global, 'XMLHttpRequest', () => require('XMLHttpRequest'));
defineProperty(global, 'FormData', () => require('FormData'));

defineProperty(global, 'fetch', () => require('fetch').fetch);
defineProperty(global, 'Headers', () => require('fetch').Headers);
defineProperty(global, 'Request', () => require('fetch').Request);
defineProperty(global, 'Response', () => require('fetch').Response);
defineProperty(global, 'WebSocket', () => require('WebSocket'));
defineProperty(global, 'Blob', () => require('Blob'));
defineProperty(global, 'URL', () => require('URL'));

// Set up alert
if (!global.alert) {
  global.alert = function(text) {
    // Require Alert on demand. Requiring it too early can lead to issues
    // with things like Platform not being fully initialized.
    require('Alert').alert('Alert', '' + text);
  };
}

// Set up Geolocation
let navigator = global.navigator;
if (navigator === undefined) {
  global.navigator = navigator = {};
}

// see https://github.com/facebook/react-native/issues/10881
defineProperty(navigator, 'product', () => 'ReactNative', true);
defineProperty(navigator, 'geolocation', () => require('Geolocation'));

// Just to make sure the JS gets packaged up. Wait until the JS environment has
// been initialized before requiring them.
const BatchedBridge = require('BatchedBridge');
BatchedBridge.registerLazyCallableModule('Systrace', () => require('Systrace'));
BatchedBridge.registerLazyCallableModule('JSTimers', () => require('JSTimers'));
BatchedBridge.registerLazyCallableModule('HeapCapture', () => require('HeapCapture'));
BatchedBridge.registerLazyCallableModule('SamplingProfiler', () => require('SamplingProfiler'));
BatchedBridge.registerLazyCallableModule('RCTLog', () => require('RCTLog'));
BatchedBridge.registerLazyCallableModule('RCTDeviceEventEmitter', () => require('RCTDeviceEventEmitter'));
BatchedBridge.registerLazyCallableModule('RCTNativeAppEventEmitter', () => require('RCTNativeAppEventEmitter'));
BatchedBridge.registerLazyCallableModule('PerformanceLogger', () => require('PerformanceLogger'));

// Set up devtools
if (__DEV__) {
  if (!global.__RCTProfileIsProfiling) {
    BatchedBridge.registerCallableModule('HMRClient', require('HMRClient'));

    // not when debugging in chrome
    // TODO(t12832058) This check is broken
    if (!window.document) {
      require('setupDevtools');
    }

    require('RCTDebugComponentOwnership');

    // Set up inspector
    const JSInspector = require('JSInspector');
    JSInspector.registerAgent(require('NetworkAgent'));
  }
}
