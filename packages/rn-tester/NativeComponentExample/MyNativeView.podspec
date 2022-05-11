# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

require "json"

package = JSON.parse(File.read(File.join(__dir__, "../" "package.json")))

folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'
folly_version = '2021.06.28.00-v2'
boost_version = '1.76.0'
boost_compiler_flags = '-Wno-documentation'

Pod::Spec.new do |s|
  s.name            = "MyNativeView"
  s.version         = package["version"]
  s.summary         = package["description"]
  s.description     = "my-native-view"
  s.homepage        = "https://github.com/sota000/my-native-view.git"
  s.license         = "MIT"
  s.platforms       = { :ios => "12.4", :tvos => "12.4" }
  s.compiler_flags  = folly_compiler_flags + ' ' + boost_compiler_flags + ' -Wno-nullability-completeness'
  s.author          = "Facebook, Inc. and its affiliates"
  s.source          = { :git => "https://github.com/facebook/my-native-view.git", :tag => "#{s.version}" }
  s.pod_target_xcconfig    = {
    "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\" \"$(PODS_ROOT)/RCT-Folly\" \"$(PODS_ROOT)/boost\" \"${PODS_CONFIGURATION_BUILD_DIR}/React-Codegen/React_Codegen.framework/Headers\"",
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
  }

  s.source_files    = "ios/**/*.{h,m,mm,cpp}"
  s.requires_arc    = true

  s.dependency "React"
  s.dependency "React-RCTFabric"
  s.dependency "React-Codegen"
  s.dependency "RCTRequired"
  s.dependency "RCTTypeSafety"
  s.dependency "ReactCommon/turbomodule/core"
end
