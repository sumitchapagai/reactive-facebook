/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow
 */

'use strict';

import type {TurboModule} from 'RCTExport';
import * as TurboModuleRegistry from 'TurboModuleRegistry';

export type PermissionStatus = 'granted' | 'denied' | 'never_ask_again';

export interface Spec extends TurboModule {
  +checkPermission: (permission: string) => Promise<boolean>;

  +requestPermission: (permission: string) => Promise<PermissionStatus>;

  +shouldShowRequestPermissionRationale: (
    permission: string,
  ) => Promise<boolean>;

  +requestMultiplePermissions: (permissions: Array<string>) => Promise<Object>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('PermissionsAndroid');
