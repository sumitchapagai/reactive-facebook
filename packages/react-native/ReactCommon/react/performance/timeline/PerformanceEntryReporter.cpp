/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "PerformanceEntryReporter.h"

#include <cxxreact/JSExecutor.h>

namespace facebook::react {

std::shared_ptr<PerformanceEntryReporter>&
PerformanceEntryReporter::getInstance() {
  static auto instance = std::make_shared<PerformanceEntryReporter>();
  return instance;
}

PerformanceEntryReporter::PerformanceEntryReporter() = default;

void PerformanceEntryReporter::setReportingCallback(
    std::function<void()> callback) {
  callback_ = std::move(callback);
}

DOMHighResTimeStamp PerformanceEntryReporter::getCurrentTimeStamp() const {
  return timeStampProvider_ != nullptr ? timeStampProvider_()
                                       : JSExecutor::performanceNow();
}

void PerformanceEntryReporter::startReporting(PerformanceEntryType entryType) {
  auto& buffer = getBuffer(entryType);
  buffer.isReporting = true;
  buffer.durationThreshold = DEFAULT_DURATION_THRESHOLD;
}

void PerformanceEntryReporter::setAlwaysLogged(
    PerformanceEntryType entryType,
    bool isAlwaysLogged) {
  auto& buffer = getBuffer(entryType);
  buffer.isAlwaysLogged = isAlwaysLogged;
}

void PerformanceEntryReporter::setDurationThreshold(
    PerformanceEntryType entryType,
    DOMHighResTimeStamp durationThreshold) {
  getBuffer(entryType).durationThreshold = durationThreshold;
}

void PerformanceEntryReporter::stopReporting(PerformanceEntryType entryType) {
  getBuffer(entryType).isReporting = false;
}

void PerformanceEntryReporter::stopReporting() {
  eventBuffer_.isReporting = false;
  markBuffer_.isReporting = false;
  measureBuffer_.isReporting = false;
}

PerformanceEntryReporter::PopPendingEntriesResult
PerformanceEntryReporter::popPendingEntries() {
  std::lock_guard lock(entriesMutex_);
  PopPendingEntriesResult res = {
      .entries = std::vector<PerformanceEntry>(),
      .droppedEntriesCount = droppedEntriesCount_};

  eventBuffer_.consume(res.entries);
  markBuffer_.consume(res.entries);
  measureBuffer_.consume(res.entries);

  // Sort by starting time (or ending time, if starting times are equal)
  std::stable_sort(
      res.entries.begin(), res.entries.end(), PerformanceEntrySorter{});

  droppedEntriesCount_ = 0;
  return res;
}

void PerformanceEntryReporter::logEntry(const PerformanceEntry& entry) {
  if (entry.entryType == PerformanceEntryType::EVENT) {
    eventCounts_[entry.name]++;
  }

  if (!isReporting(entry.entryType) && !isAlwaysLogged(entry.entryType)) {
    return;
  }

  std::lock_guard lock(entriesMutex_);

  auto& buffer = getBuffer(entry.entryType);

  if (entry.duration < buffer.durationThreshold) {
    // The entries duration is lower than the desired reporting threshold, skip
    return;
  }

  auto pushResult = buffer.add(std::move(entry));
  if (pushResult ==
      BoundedConsumableBuffer<PerformanceEntry>::PushStatus::DROP) {
    // Start dropping entries once reached maximum buffer size.
    // The number of dropped entries will be reported back to the corresponding
    // PerformanceObserver callback.
    droppedEntriesCount_ += 1;
  }

  if (buffer.pendingMessagesCount() == 1) {
    // If the buffer was empty, it signals that JS side just has possibly
    // consumed it and is ready to get more
    scheduleFlushBuffer();
  }
}

void PerformanceEntryReporter::mark(
    const std::string& name,
    const std::optional<DOMHighResTimeStamp>& startTime) {
  logEntry(PerformanceEntry{
      .name = name,
      .entryType = PerformanceEntryType::MARK,
      .startTime = startTime ? *startTime : getCurrentTimeStamp()});
}

void PerformanceEntryReporter::clearEntries(
    std::optional<PerformanceEntryType> entryType,
    std::string_view entryName) {
  // Clear all entry types
  if (!entryType) {
    for (int i = 1; i < NUM_PERFORMANCE_ENTRY_TYPES; i++) {
      clearEntries(static_cast<PerformanceEntryType>(i), entryName);
    }

    return;
  }

  auto& buffer = getBuffer(*entryType);
  if (!entryName.empty()) {
    std::lock_guard lock(entriesMutex_);
    buffer.clear(entryName);
  } else {
    std::lock_guard lock(entriesMutex_);
    buffer.clear();
  }
}

void PerformanceEntryReporter::getEntries(
    PerformanceEntryType entryType,
    std::string_view entryName,
    std::vector<PerformanceEntry>& res) const {
  std::lock_guard lock(entriesMutex_);
  auto& buffer = getBuffer(entryType);

  if (entryName.empty()) {
    buffer.getEntries(std::nullopt, res);
  } else {
    buffer.getEntries(entryName, res);
  }
}

std::vector<PerformanceEntry> PerformanceEntryReporter::getEntries(
    std::optional<PerformanceEntryType> entryType,
    std::string_view entryName) const {
  std::vector<PerformanceEntry> res;
  if (!entryType) {
    // Collect all entry types
    for (int i = 1; i < NUM_PERFORMANCE_ENTRY_TYPES; i++) {
      getEntries(static_cast<PerformanceEntryType>(i), entryName, res);
    }
  } else {
    getEntries(*entryType, entryName, res);
  }
  return res;
}

void PerformanceEntryReporter::measure(
    const std::string& name,
    DOMHighResTimeStamp startTime,
    DOMHighResTimeStamp endTime,
    const std::optional<DOMHighResTimeStamp>& duration,
    const std::optional<std::string>& startMark,
    const std::optional<std::string>& endMark) {
  DOMHighResTimeStamp startTimeVal =
      startMark ? getMarkTime(*startMark) : startTime;
  DOMHighResTimeStamp endTimeVal = endMark ? getMarkTime(*endMark) : endTime;

  if (!endMark && endTime < startTimeVal) {
    // The end time is not specified, take the current time, according to the
    // standard
    endTimeVal = getCurrentTimeStamp();
  }

  DOMHighResTimeStamp durationVal =
      duration ? *duration : endTimeVal - startTimeVal;

  logEntry(
      {.name = name,
       .entryType = PerformanceEntryType::MEASURE,
       .startTime = startTimeVal,
       .duration = durationVal});
}

DOMHighResTimeStamp PerformanceEntryReporter::getMarkTime(
    const std::string& markName) const {
  std::lock_guard lock(entriesMutex_);

  if (auto it = markBuffer_.entries.find(markName); it) {
    return it->startTime;
  } else {
    return 0.0;
  }
}

void PerformanceEntryReporter::logEventEntry(
    std::string name,
    DOMHighResTimeStamp startTime,
    DOMHighResTimeStamp duration,
    DOMHighResTimeStamp processingStart,
    DOMHighResTimeStamp processingEnd,
    uint32_t interactionId) {
  logEntry(
      {.name = std::move(name),
       .entryType = PerformanceEntryType::EVENT,
       .startTime = startTime,
       .duration = duration,
       .processingStart = processingStart,
       .processingEnd = processingEnd,
       .interactionId = interactionId});
}

void PerformanceEntryReporter::scheduleFlushBuffer() {
  if (callback_) {
    callback_();
  }
}

} // namespace facebook::react
