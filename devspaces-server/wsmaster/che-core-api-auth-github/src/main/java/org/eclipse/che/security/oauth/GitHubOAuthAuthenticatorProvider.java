/*
 * Copyright (c) 2012-2023 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.security.oauth;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.commons.lang.StringUtils.trimEnd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.che.commons.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of GitHub {@link OAuthAuthenticator} based on available configuration.
 *
 * @author Pavol Baran
 */
@Singleton
public class GitHubOAuthAuthenticatorProvider implements Provider<OAuthAuthenticator> {
  private static final Logger LOG = LoggerFactory.getLogger(GitHubOAuthAuthenticatorProvider.class);
  private final OAuthAuthenticator authenticator;

  @Inject
  public GitHubOAuthAuthenticatorProvider(
      @Nullable @Named("che.oauth2.github.clientid_filepath") String gitHubClientIdPath,
      @Nullable @Named("che.oauth2.github.clientsecret_filepath") String gitHubClientSecretPath,
      @Nullable @Named("che.oauth.github.redirecturis") String[] redirectUris,
      @Nullable @Named("che.integration.github.oauth_endpoint") String oauthEndpoint,
      @Nullable @Named("che.oauth.github.authuri") String authUri,
      @Nullable @Named("che.oauth.github.tokenuri") String tokenUri)
      throws IOException {
    authenticator =
        getOAuthAuthenticator(
            gitHubClientIdPath,
            gitHubClientSecretPath,
            redirectUris,
            oauthEndpoint,
            authUri,
            tokenUri);
    LOG.debug("{} GitHub OAuth Authenticator is used.", authenticator);
  }

  @Override
  public OAuthAuthenticator get() {
    return authenticator;
  }

  private OAuthAuthenticator getOAuthAuthenticator(
      String clientIdPath,
      String clientSecretPath,
      String[] redirectUris,
      String oauthEndpoint,
      String authUri,
      String tokenUri)
      throws IOException {

    String trimmedOauthEndpoint = isNullOrEmpty(oauthEndpoint) ? null : trimEnd(oauthEndpoint, '/');
    authUri =
        isNullOrEmpty(trimmedOauthEndpoint)
            ? authUri
            : trimmedOauthEndpoint + "/login/oauth/authorize";
    tokenUri =
        isNullOrEmpty(trimmedOauthEndpoint)
            ? tokenUri
            : trimmedOauthEndpoint + "/login/oauth/access_token";
    if (!isNullOrEmpty(clientIdPath)
        && !isNullOrEmpty(clientSecretPath)
        && !isNullOrEmpty(authUri)
        && !isNullOrEmpty(tokenUri)
        && Objects.nonNull(redirectUris)
        && redirectUris.length != 0) {
      final String clientId = Files.readString(Path.of(clientIdPath)).trim();
      final String clientSecret = Files.readString(Path.of(clientSecretPath)).trim();
      if (!isNullOrEmpty(clientId) && !isNullOrEmpty(clientSecret)) {
        return new GitHubOAuthAuthenticator(
            clientId, clientSecret, redirectUris, trimmedOauthEndpoint, authUri, tokenUri);
      }
    }
    return new NoopOAuthAuthenticator();
  }

  static class NoopOAuthAuthenticator extends OAuthAuthenticator {
    @Override
    public String getOAuthProvider() {
      return "Noop";
    }

    @Override
    public String getEndpointUrl() {
      return "Noop";
    }
  }
}
