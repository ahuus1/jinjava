/**********************************************************************
Copyright (c) 2014 HubSpot Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 **********************************************************************/
package com.hubspot.jinjava.lib.filter;

import java.math.BigDecimal;

import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.util.Objects;

public class AddFilter implements Filter {

  @Override
  public Object filter(Object object, JinjavaInterpreter interpreter, String... arg) {
    if (arg.length != 1) {
      throw new InterpretException("filter add expects 1 arg >>> " + arg.length);
    }

    try {
      BigDecimal base = new BigDecimal(Objects.toString(object));
      BigDecimal addend = new BigDecimal(Objects.toString(arg[0]));
  
      return base.add(addend);
    }
    catch(Exception e) {
      throw new InterpretException("filter add error", e);
    }
  }

  @Override
  public String getName() {
    return "add";
  }

}
