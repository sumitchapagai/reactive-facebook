/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef RCTDATAREQUESTHANDLER_H
#define RCTDATAREQUESTHANDLER_H

#import <React/RCTInvalidating.h>
#import <React/RCTURLRequestHandler.h>

/**
 * This is the default RCTURLRequestHandler implementation for data URL requests.
 */
@interface RCTDataRequestHandler : NSObject <RCTURLRequestHandler, RCTInvalidating>

@end

#endif //RCTDATAREQUESTHANDLER_H
