/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.objectserver.dgc.api;

import com.tc.objectserver.api.GCStats;
import com.tc.util.State;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class GCStatsImpl implements GCStats, Serializable {
  private static final long      serialVersionUID      = -4177683133067698672L;

  private static final long      NOT_INITIALIZED       = -1L;
  private static final String    YOUNG_GENERATION      = "Young";
  private static final String    FULL_GENERATION       = "Full";
  private final int              number;
  private final long             startTime;
  private final String           startTimeFormatted;
  private long                   elapsedTime           = NOT_INITIALIZED;
  private long                   beginObjectCount      = NOT_INITIALIZED;
  private long                   endObjectCount        = NOT_INITIALIZED;
  private long                   candidateGarbageCount = NOT_INITIALIZED;
  private long                   actualGarbageCount    = NOT_INITIALIZED;
  private long                   markStageTime         = NOT_INITIALIZED;
  private long                   pausedStageTime       = NOT_INITIALIZED;
  private State                  state                 = GCStats.GC_START;
  private final boolean          fullGC;

  public GCStatsImpl(int number, boolean fullGC, long startTime) {
    this.number = number;
    this.fullGC = fullGC;
    this.startTime = startTime;
    this.startTimeFormatted = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z").format(startTime);
  }

  @Override
  public int getIteration() {
    return this.number;
  }

  public synchronized void setMarkState() {
    this.state = GC_MARK;
  }

  public synchronized void setPauseState() {
    this.state = GC_PAUSE;
  }

  public synchronized void setMarkCompleteState() {
    this.state = GC_MARK_COMPLETE;
  }

  public synchronized void setCompleteState() {
    this.state = GC_COMPLETE;
  }

  public synchronized void setCanceledState() {
    this.state = GC_CANCELED;
  }

  @Override
  public synchronized long getStartTime() {
    return this.startTime;
  }

  @Override
  public synchronized long getElapsedTime() {
    return this.elapsedTime;
  }

  @Override
  public synchronized long getBeginObjectCount() {
    return this.beginObjectCount;
  }

  @Override
  public synchronized long getEndObjectCount() {
    return this.endObjectCount;
  }

  @Override
  public synchronized long getCandidateGarbageCount() {
    return this.candidateGarbageCount;
  }

  @Override
  public synchronized long getActualGarbageCount() {
    return this.actualGarbageCount;
  }

  @Override
  public synchronized long getMarkStageTime() {
    return this.markStageTime;
  }

  @Override
  public synchronized long getPausedStageTime() {
    return this.pausedStageTime;
  }

  @Override
  public synchronized String getStatus() {
    return state.getName();
  }

  @Override
  public synchronized String getType() {
    return fullGC ? FULL_GENERATION : YOUNG_GENERATION;
  }

  public synchronized void setActualGarbageCount(long count) {
    this.actualGarbageCount = count;
  }

  public synchronized void setBeginObjectCount(long count) {
    this.beginObjectCount = count;
  }

  public synchronized void setEndObjectCount(long count) {
    this.endObjectCount = count;
  }

  public synchronized void setCandidateGarbageCount(long count) {
    this.candidateGarbageCount = count;
  }

  public synchronized void setMarkStageTime(long time) {
    this.markStageTime = time;
  }

  public synchronized void setPausedStageTime(long time) {
    this.pausedStageTime = time;
  }

  public synchronized void setElapsedTime(long time) {
    this.elapsedTime = time;
  }

  private String formatTime(long time) {
    if (time == NOT_INITIALIZED) {
      return "N/A";
    } else {
      return time + "ms";
    }
  }

  @Override
  public String toString() {
    return "DGCStats[ iteration: " + getIteration() + "; type: " + getType() + "; status: " + getStatus()
           + " ] : startTime = " + this.startTimeFormatted + "; elapsedTime = " + formatTime(this.elapsedTime)
           + "; markStageTime = " + formatTime(markStageTime) + "; pausedStageTime = "
           + formatTime(this.pausedStageTime) + "; beginObjectCount = " + this.beginObjectCount + "; endObjectCount = "
           + this.endObjectCount + "; candidateGarbageCount = " + this.candidateGarbageCount +
           "; actualGarbageCount = " + this.actualGarbageCount;
  }

}
