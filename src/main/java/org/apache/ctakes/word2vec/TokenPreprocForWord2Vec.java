package org.apache.ctakes.word2vec;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;

public class TokenPreprocForWord2Vec {

  /**
   * Determine what to print based on the token's type.
   */
  public static String tokenToString(BaseToken token) {

    String stringValue;
    String tokenType = token.getClass().getSimpleName();
    String tokenText = token.getCoveredText().toLowerCase();

    switch(tokenType) {
    case "ContractionToken":
      stringValue = tokenText;
      break;
    case "NewlineToken":
      stringValue = null;
      break;
    case "NumToken":
      stringValue = "number_token";
      break;
    case "PunctuationToken":
      stringValue = tokenText;
      break;
    case "SymbolToken":
      stringValue = tokenText;
      break;
    case "WordToken":
      stringValue = tokenText;
      break;
    default:
      throw new IllegalArgumentException("Invalid token type: " + tokenType);
    }

    return stringValue;
  }
}
