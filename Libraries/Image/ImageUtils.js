/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow
 * @format
 */

type ResizeMode = 'cover' | 'contain' | 'stretch' | 'repeat' | 'center';

export function convertObjectFitToResizeMode(objectFit?: ?string): ?ResizeMode {
  switch (objectFit) {
    case 'contain':
    case 'scale-down':
      return 'contain';
    case 'cover':
      return 'cover';
    case 'fill':
      return 'stretch';
    default:
      return null;
  }
}
