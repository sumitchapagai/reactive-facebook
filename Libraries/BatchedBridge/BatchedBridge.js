/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule BatchedBridge
 */
'use strict';

const MessageQueue = require('MessageQueue');

const BatchedBridge = new MessageQueue(
  __fbBatchedBridgeConfig.remoteModuleConfig,
  __fbBatchedBridgeConfig.localModulesConfig,
);

// TODO: Move these around to solve the cycle in a cleaner way.

const BridgeProfiling = require('BridgeProfiling');
const JSTimersExecution = require('JSTimersExecution');

BatchedBridge.registerCallableModule('BridgeProfiling', BridgeProfiling);
BatchedBridge.registerCallableModule('JSTimersExecution', JSTimersExecution);

// Wire up the batched bridge on the global object so that we can call into it.
// Ideally, this would be the inverse relationship. I.e. the native environment
// provides this global directly with its script embedded. Then this module
// would export it. A possible fix would be to trim the dependencies in
// MessageQueue to its minimal features and embed that in the native runtime.

Object.defineProperty(global, '__fbBatchedBridge', { value: BatchedBridge });

module.exports = BatchedBridge;
