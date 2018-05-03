// Copyright 2004-present Facebook. All Rights Reserved.

#pragma once

#include <cstdint>
#include <functional>
#include <memory>
#include <unordered_map>
#include <utility>

#include <cxxreact/JSModulesUnbundle.h>
#include <jschelpers/noncopyable.h>

#ifndef RN_EXPORT
#define RN_EXPORT __attribute__((visibility("default")))
#endif

namespace facebook {
namespace react {

class RN_EXPORT RAMBundleRegistry : noncopyable {
public:
  constexpr static uint32_t MAIN_BUNDLE_ID = 0;

  static std::unique_ptr<RAMBundleRegistry> singleBundleRegistry(
      std::unique_ptr<JSModulesUnbundle> mainBundle);
  static std::unique_ptr<RAMBundleRegistry> multipleBundlesRegistry(
      std::unique_ptr<JSModulesUnbundle> mainBundle,
      std::function<std::unique_ptr<JSModulesUnbundle>(std::string)> factory);

  explicit RAMBundleRegistry(
      std::unique_ptr<JSModulesUnbundle> mainBundle,
      std::function<
        std::unique_ptr<JSModulesUnbundle>(std::string)> factory = nullptr);

  RAMBundleRegistry(RAMBundleRegistry&&) = default;
  RAMBundleRegistry& operator=(RAMBundleRegistry&&) = default;

  void registerBundle(uint32_t bundleId, std::string bundlePath);
  JSModulesUnbundle::Module getModule(uint32_t bundleId, uint32_t moduleId);
  virtual ~RAMBundleRegistry() {};
private:
  JSModulesUnbundle* getBundle(uint32_t bundleId) const;

  std::function<std::unique_ptr<JSModulesUnbundle>(std::string)> m_factory;
  std::unordered_map<uint32_t, std::string> m_bundlePaths;
  std::unordered_map<uint32_t, std::unique_ptr<JSModulesUnbundle>> m_bundles;
};

}  // namespace react
}  // namespace facebook
