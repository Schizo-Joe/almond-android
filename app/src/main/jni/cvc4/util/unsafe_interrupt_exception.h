/*********************                                                        */
/*! \file unsafe_interrupt_exception.h
 ** \verbatim
 ** Top contributors (to current version):
 **   Liana Hadarean, Paul Meng, Tim King
 ** This file is part of the CVC4 project.
 ** Copyright (c) 2009-2017 by the authors listed in the file AUTHORS
 ** in the top-level source directory) and their institutional affiliations.
 ** All rights reserved.  See the file COPYING in the top-level source
 ** directory for licensing information.\endverbatim
 **
 ** \brief An exception that is thrown when the solver is out of time/resources
 ** and is interrupted in an unsafe state
 **/

#include <cvc4/cvc4_public.h>

#ifndef __CVC4__UNSAFE_INTERRUPT_EXCEPTION_H
#define __CVC4__UNSAFE_INTERRUPT_EXCEPTION_H

#include <cvc4/base/exception.h>

namespace CVC4 {

class CVC4_PUBLIC UnsafeInterruptException : public CVC4::Exception {
public:
  UnsafeInterruptException() :
    Exception("Interrupted in unsafe state due to "
              "time/resource limit.") {
  }

  UnsafeInterruptException(const std::string& msg) :
    Exception(msg) {
  }

  UnsafeInterruptException(const char* msg) :
    Exception(msg) {
  }
};/* class UnsafeInterruptException */

}/* CVC4 namespace */

#endif /* __CVC4__UNSAFE_INTERRUPT_EXCEPTION_H */
