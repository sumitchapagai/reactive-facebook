/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import <React/RCTBridge.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTURLRequestHandler.h>

@interface RCTBlobManager : NSObject <RCTBridgeModule, RCTURLRequestHandler>

- (NSString *)store:(NSData *)data;

- (NSData *)resolve:(NSDictionary<NSString *, id> *)blob;

- (NSData *)resolve:(NSString *)blobId offset:(NSInteger)offset size:(NSInteger)size;

- (void)release:(NSString *)blobId;

@end


@interface RCTBridge (RCTBlobManager)

@property (nonatomic, readonly) RCTBlobManager *blobs;

@end
