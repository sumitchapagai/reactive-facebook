// Copyright (c) Facebook, Inc. and its affiliates.

// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

#pragma once

#include <fabric/core/ShadowNode.h>
#include <fabric/uimanager/ShadowViewMutation.h>

namespace facebook {
namespace react {

/*
 * Calculates a list of view mutations which describes how the old
 * `ShadowTree` can be transformed to the new one.
 * The list of mutations might be and might not be optimal.
 */
ShadowViewMutationList calculateShadowViewMutations(
  const ShadowNode &oldRootShadowNode,
  const ShadowNode &newRootShadowNode
);

} // namespace react
} // namespace facebook
