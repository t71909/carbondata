/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.core.memory;

/**
 * Holds the pointers for rows.
 */
public class IntPointerBuffer {

  private int length;

  private int actualSize;

  private int[] pointerBlock;

  private MemoryBlock baseBlock;

  private MemoryBlock pointerMemoryBlock;

  public IntPointerBuffer(MemoryBlock baseBlock) {
    // TODO can be configurable, it is initial size and it can grow automatically.
    this.length = 100000;
    pointerBlock = new int[length];
    this.baseBlock = baseBlock;
  }

  public IntPointerBuffer(int length) {
    this.length = length;
    pointerBlock = new int[length];
  }

  public void set(int rowId, int value) {
    pointerBlock[rowId] = value;
  }

  public void set(int value) {
    ensureMemory();
    pointerBlock[actualSize] = value;
    actualSize++;
  }

  /**
   * Returns the value at position {@code rowId}.
   */
  public int get(int rowId) {
    assert rowId >= 0 : "rowId (" + rowId + ") should >= 0";
    assert rowId < length : "rowId (" + rowId + ") should < length (" + length + ")";
    if (pointerBlock == null) {
      return CarbonUnsafe.unsafe.getInt(pointerMemoryBlock.getBaseObject(),
          pointerMemoryBlock.getBaseOffset() + (rowId << 2));
    }
    return pointerBlock[rowId];
  }

  public void loadToUnsafe() throws MemoryException {
    pointerMemoryBlock = UnsafeMemoryManager.allocateMemoryWithRetry(pointerBlock.length * 4);
    for (int i = 0; i < pointerBlock.length; i++) {
      CarbonUnsafe.unsafe
          .putInt(pointerMemoryBlock.getBaseObject(), pointerMemoryBlock.getBaseOffset() + i * 4,
              pointerBlock[i]);
    }
    pointerBlock = null;
  }

  public int getActualSize() {
    return actualSize;
  }

  public MemoryBlock getBaseBlock() {
    return baseBlock;
  }

  public int[] getPointerBlock() {
    return pointerBlock;
  }

  private void ensureMemory() {
    if (actualSize >= length) {
      // Expand by quarter, may be we can correct the logic later
      int localLength = length + (int) (length * (0.25));
      int[] memoryAddress = new int[localLength];
      System.arraycopy(pointerBlock, 0, memoryAddress, 0, length);
      pointerBlock = memoryAddress;
      length = localLength;
    }
  }

  public void freeMemory() {
    pointerBlock = null;
    if (pointerMemoryBlock != null) {
      UnsafeMemoryManager.INSTANCE.freeMemory(pointerMemoryBlock);
    }
    if (baseBlock != null) {
      UnsafeMemoryManager.INSTANCE.freeMemory(baseBlock);
    }
  }
}