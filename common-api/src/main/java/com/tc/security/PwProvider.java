package com.tc.security;

import java.net.URI;

/**
* @author Alex Snaps
*/
public interface PwProvider {
  char[] getPasswordFor(URI uri);
}
