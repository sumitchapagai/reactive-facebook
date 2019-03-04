/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <UIKit/UIKit.h>

#import <React/RCTMountItemProtocol.h>
#import <React/RCTPrimitives.h>
#import <react/core/ReactPrimitives.h>

NS_ASSUME_NONNULL_BEGIN

@class RCTComponentViewRegistry;

/**
 * Creates a ready-to-mount component view.
 */
@interface RCTCreateMountItem : NSObject <RCTMountItemProtocol>

- (instancetype)initWithComponentHandle:(facebook::react::ComponentHandle)componentHandle tag:(ReactTag)tag;

@end

NS_ASSUME_NONNULL_END
