/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict
 * @format
 */

'use strict';

import type {OptionsShape} from '../../../CodegenSchema.js';

// $FlowFixMe[unclear-type] there's no flowtype for ASTs
type OptionsAST = Object;

function getOptions(optionsExpression: OptionsAST): ?OptionsShape {
  if (!optionsExpression) {
    return null;
  }
  let foundOptions;
  try {
    foundOptions = optionsExpression.properties.reduce((options, prop) => {
      if (prop.value.type === 'ArrayExpression') {
        options[prop.key.name] = prop.value.elements.map(
          element => element.value,
        );
      } else {
        options[prop.key.name] = prop.value.value;
      }
      return options;
    }, {});
  } catch (e) {
    throw new Error(
      'Failed to parse codegen options, please check that they are defined correctly',
    );
  }

  if (
    foundOptions.paperComponentName &&
    foundOptions.paperComponentNameDeprecated
  ) {
    throw new Error(
      'Failed to parse codegen options, cannot use both paperComponentName and paperComponentNameDeprecated',
    );
  }

  return foundOptions;
}

module.exports = {
  getOptions,
};
