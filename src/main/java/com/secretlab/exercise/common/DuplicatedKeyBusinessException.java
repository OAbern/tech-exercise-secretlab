package com.secretlab.exercise.common;

import lombok.NoArgsConstructor;

/**
 * customer duplicated key exception
 *
 * @author fengdi
 * @date 2026/5/20
 */
@NoArgsConstructor
public class DuplicatedKeyBusinessException extends RuntimeException {

  public DuplicatedKeyBusinessException(String msg) {
    super(msg);
  }
}
