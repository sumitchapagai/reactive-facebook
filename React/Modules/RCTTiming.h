/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef RCTTIMING_H
#define RCTTIMING_H

#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTFrameUpdate.h>
#import <React/RCTInvalidating.h>

@interface RCTTiming : NSObject <RCTBridgeModule, RCTInvalidating, RCTFrameUpdateObserver>

@end

#endif //RCTTIMING_H
