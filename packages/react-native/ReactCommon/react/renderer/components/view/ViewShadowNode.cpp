/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "ViewShadowNode.h"
#include <react/config/ReactNativeConfig.h>
#include <react/renderer/components/view/ViewTraitsInitializer.h>
#include <react/renderer/components/view/primitives.h>
#include <react/utils/CoreFeatures.h>

namespace facebook::react {

char const ViewComponentName[] = "View";

ViewShadowNodeProps::ViewShadowNodeProps(
    PropsParserContext const &context,
    ViewShadowNodeProps const &sourceProps,
    RawProps const &rawProps)
    : ViewProps(
          context,
          sourceProps,
          rawProps,
          !CoreFeatures::enableMapBuffer){};

ViewShadowNode::ViewShadowNode(
    ShadowNodeFragment const &fragment,
    ShadowNodeFamily::Shared const &family,
    ShadowNodeTraits traits)
    : ConcreteViewShadowNode(fragment, family, traits) {
  initialize();
}

ViewShadowNode::ViewShadowNode(
    ShadowNode const &sourceShadowNode,
    ShadowNodeFragment const &fragment)
    : ConcreteViewShadowNode(sourceShadowNode, fragment) {
  initialize();
}

void ViewShadowNode::initialize() noexcept {
  auto &viewProps = static_cast<ViewProps const &>(*props_);

  bool formsStackingContext = !viewProps.collapsable ||
      viewProps.pointerEvents == PointerEventsMode::None ||
      !viewProps.nativeId.empty() || viewProps.accessible ||
      viewProps.opacity != 1.0 || viewProps.transform != Transform{} ||
      (viewProps.zIndex.has_value() &&
       viewProps.yogaStyle.positionType() != YGPositionTypeStatic) ||
      viewProps.yogaStyle.display() == YGDisplayNone ||
      viewProps.getClipsContentToBounds() || viewProps.events.bits.any() ||
      isColorMeaningful(viewProps.shadowColor) ||
      viewProps.accessibilityElementsHidden ||
      viewProps.accessibilityViewIsModal ||
      viewProps.importantForAccessibility != ImportantForAccessibility::Auto ||
      viewProps.removeClippedSubviews ||
      ViewTraitsInitializer::formsStackingContext(viewProps);

  bool formsView = formsStackingContext ||
      isColorMeaningful(viewProps.backgroundColor) ||
      !(viewProps.yogaStyle.border() == YGStyle::Edges{}) ||
      !viewProps.testId.empty() || ViewTraitsInitializer::formsView(viewProps);

  if (formsView) {
    traits_.set(ShadowNodeTraits::Trait::FormsView);
  } else {
    traits_.unset(ShadowNodeTraits::Trait::FormsView);
  }

  if (formsStackingContext) {
    traits_.set(ShadowNodeTraits::Trait::FormsStackingContext);
  } else {
    traits_.unset(ShadowNodeTraits::Trait::FormsStackingContext);
  }

  traits_.set(ViewTraitsInitializer::extraTraits());
}

} // namespace facebook::react
