/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

#import <XCTest/XCTest.h>

#import <RCTBlob/RCTBlobManager.h>

@interface RCTBlobManagerTests : XCTestCase

@end

@implementation RCTBlobManagerTests
{
  RCTBlobManager *_module;
  NSMutableData *_data;
  NSString *_blobId;
}

- (void)setUp
{
  [super setUp];
  
  _module = [RCTBlobManager new];
  NSInteger size = 120;
  _data = [NSMutableData dataWithCapacity:size];
  for (NSInteger i = 0; i < size / 4; i++) {
    uint32_t randomBits = arc4random();
    [_data appendBytes:(void *)&randomBits length:4];
  }
  _blobId = [NSUUID UUID].UUIDString;
  [_module store:_data withId:_blobId];
}

- (void)testResolve
{
  XCTAssertTrue([_data isEqualToData:[_module resolve:_blobId offset:0 size:_data.length]]);
  NSData *rangeData = [_data subdataWithRange:NSMakeRange(30, _data.length - 30)];
  XCTAssertTrue([rangeData isEqualToData:[_module resolve:_blobId offset:30 size:_data.length - 30]]);
}

- (void)testResolveMap
{
  NSDictionary *map = @{
    @"blobId": _blobId,
    @"size": @(_data.length),
    @"offset": @(0),
  };
  XCTAssertTrue([_data isEqualToData:[_module resolve:map]]);
}

- (void)testRemove
{
  XCTAssertNotNil([_module resolve:_blobId offset:0 size:_data.length]);
  [_module remove:_blobId];
  XCTAssertNil([_module resolve:_blobId offset:0 size:_data.length]);
}

- (void)testCreateFromParts
{
  NSDictionary *blobData = @{
    @"blobId": _blobId,
    @"offset": @(0),
    @"size": @(_data.length),
  };
  NSDictionary *blob = @{
   @"data": blobData,
   @"type": @"blob",
  };
  NSString *stringData = @"i ♥ dogs";
  NSDictionary *string = @{
    @"data": stringData,
    @"type": @"string",
  };
  NSString *resultId = [NSUUID UUID].UUIDString;
  NSArray *parts = @[blob, string];
  
  [_module createFromParts:parts withId:resultId];
  
  NSMutableData *expectedData = [NSMutableData new];
  [expectedData appendData:_data];
  [expectedData appendData:[stringData dataUsingEncoding:NSUTF8StringEncoding]];
  
  NSData *result = [_module resolve:resultId offset:0 size:expectedData.length];
  
  XCTAssertTrue([expectedData isEqualToData:result]);
}

@end

