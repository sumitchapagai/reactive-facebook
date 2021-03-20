/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow strict
 */

export type AccessibilityServiceInfo = $ReadOnly<{|
  id: string,
  capabilities: string,
  eventTypes: string,
  feedbackType: string,
  notificationTimeout: number,
  flags: string,
|}>;
