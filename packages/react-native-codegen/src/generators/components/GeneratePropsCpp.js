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

import type {ComponentShape, SchemaType} from '../../CodegenSchema';

const {convertDefaultTypeToString, getImports} = require('./CppHelpers');

// File path -> contents
type FilesOutput = Map<string, string>;

const FileTemplate = ({
  libraryName,
  imports,
  componentClasses,
}: {
  libraryName: string,
  imports: string,
  componentClasses: string,
}) => `
/**
 * This code was generated by [react-native-codegen](https://www.npmjs.com/package/react-native-codegen).
 *
 * Do not edit this file as changes may cause incorrect behavior and will be lost
 * once the code is regenerated.
 *
 * ${'@'}generated by codegen project: GeneratePropsCpp.js
 */

#include <react/renderer/components/${libraryName}/Props.h>
${imports}

namespace facebook::react {

${componentClasses}

} // namespace facebook::react
`;

const ComponentTemplate = ({
  className,
  extendClasses,
  props,
}: {
  className: string,
  extendClasses: string,
  props: string,
}) =>
  `
${className}::${className}(
    const PropsParserContext &context,
    const ${className} &sourceProps,
    const RawProps &rawProps):${extendClasses}

    ${props}
      {}
`.trim();

function generatePropsString(componentName: string, component: ComponentShape) {
  return component.props
    .map(prop => {
      const defaultValue = convertDefaultTypeToString(componentName, prop);
      return `${prop.name}(convertRawProp(context, rawProps, "${prop.name}", sourceProps.${prop.name}, {${defaultValue}}))`;
    })
    .join(',\n' + '    ');
}

function getClassExtendString(component: ComponentShape): string {
  const extendString =
    ' ' +
    component.extendsProps
      .map(extendProps => {
        switch (extendProps.type) {
          case 'ReactNativeBuiltInType':
            switch (extendProps.knownTypeName) {
              case 'ReactNativeCoreViewProps':
                return 'ViewProps(context, sourceProps, rawProps)';
              default:
                (extendProps.knownTypeName: empty);
                throw new Error('Invalid knownTypeName');
            }
          default:
            (extendProps.type: empty);
            throw new Error('Invalid extended type');
        }
      })
      .join(', ') +
    `${component.props.length > 0 ? ',' : ''}`;

  return extendString;
}

module.exports = {
  generate(
    libraryName: string,
    schema: SchemaType,
    packageName?: string,
    assumeNonnull: boolean = false,
  ): FilesOutput {
    const fileName = 'Props.cpp';
    const allImports: Set<string> = new Set([
      '#include <react/renderer/core/propsConversions.h>',
      '#include <react/renderer/core/PropsParserContext.h>',
    ]);

    const componentProps = Object.keys(schema.modules)
      .map(moduleName => {
        const module = schema.modules[moduleName];
        if (module.type !== 'Component') {
          return;
        }

        const {components} = module;
        // No components in this module
        if (components == null) {
          return null;
        }

        return Object.keys(components)
          .map(componentName => {
            const component = components[componentName];
            const newName = `${componentName}Props`;

            const propsString = generatePropsString(componentName, component);
            const extendString = getClassExtendString(component);

            const imports = getImports(component.props);
            // $FlowFixMe[method-unbinding] added when improving typing for this parameters
            imports.forEach(allImports.add, allImports);

            const replacedTemplate = ComponentTemplate({
              className: newName,
              extendClasses: extendString,
              props: propsString,
            });

            return replacedTemplate;
          })
          .join('\n');
      })
      .filter(Boolean)
      .join('\n');

    const replacedTemplate = FileTemplate({
      componentClasses: componentProps,
      libraryName,
      imports: Array.from(allImports).sort().join('\n').trim(),
    });

    return new Map([[fileName, replacedTemplate]]);
  },
};
