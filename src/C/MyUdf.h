/*
 * Copyright 2018 Christian Arnault
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

#ifndef __MyUdf_h__
#define __MyUdf_h__

extern "C" {
  double mymultiply(double x, double y);
  void myarraymultiply(double array[], int arraylen);
}

extern "C" {
  int mysum(int x, int y);
}

// Structure used by pointer

typedef struct _Point {
  double x, y, z;
} Point;

extern "C" {
  Point* translate(Point* pt, double dx, double dy, double dz);
  void modify(int* ptr);
}

extern "C" {
  const char* myconcat (const char* a, const char* b);
  void myfree(const void* str);
}


#endif
