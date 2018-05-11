package com.microsoft.identity.common.internal.providers.oauth2;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
public abstract class OAuth2Strategy
        <GenericAccessToken extends AccessToken,
                GenericAccount extends Account,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericAuthorizationResponse extends AuthorizationResponse,
                GenericAuthorizationStrategy extends AuthorizationStrategy,
                GenericOAuth2Configuration extends OAuth2Configuration,
                GenericRefreshToken extends RefreshToken,
                GenericTokenRequest extends TokenRequest,
                GenericTokenResponse extends TokenResponse,
                GenericTokenResult extends TokenResult> {

    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    protected Uri mIssuer;

    protected static final String TOKEN_REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    public OAuth2Strategy(GenericOAuth2Configuration config) {

    }

    /**
     * Template method for executing an OAuth2 authorization request
     *
     * @param request
     * @param authorizationStrategy
     * @return
     */
    public GenericAuthorizationResponse requestAuthorization(
            final GenericAuthorizationRequest request,
            final GenericAuthorizationStrategy authorizationStrategy) {
        validateAuthorizationRequest(request);
        Uri authorizationUri = createAuthorizationUri();
        AuthorizationResult result = authorizationStrategy.requestAuthorization(request);
        //TODO: Reconcile authorization result and response
        AuthorizationResponse response = new AuthorizationResponse();
        return (GenericAuthorizationResponse) response;
    }


    public GenericTokenResult requestToken(final GenericTokenRequest request) throws IOException {
        validateTokenRequest(request);
        HttpResponse response = performTokenRequest(request);
        return getTokenResultFromHttpResponse(response);
    }


    protected HttpResponse performTokenRequest(final GenericTokenRequest request) throws IOException {

        String requestBody = ObjectMapper.serializeObjectToFormUrlEncoded(request);
        Map<String, String> headers = new TreeMap<>();
        String correlationId = UUID.randomUUID().toString();
        headers.put("client-request-id", correlationId);

        return HttpRequest.sendPost(
                new URL(mTokenEndpoint),
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME),
                TOKEN_REQUEST_CONTENT_TYPE
        );
    }


    /**
     * Construct the authorization endpoint URI based on issuer and path to the authorization endpoint
     * NOTE: We could look at basing this on the contents returned from the OpenID Configuration document
     *
     * @return
     */
    protected Uri createAuthorizationUri() {
        //final Uri.Builder builder = new Uri.Builder().scheme(originalAuthority.getProtocol()).authority(host).appendPath(path);
        Uri authorizationUri = Uri.withAppendedPath(mIssuer, mAuthorizationEndpoint);
        return authorizationUri;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response
     *
     * @return
     */
    public abstract String getIssuerCacheIdentifier(GenericAuthorizationRequest request);

    public abstract GenericAccessToken getAccessTokenFromResponse(GenericTokenResponse response);

    public abstract GenericRefreshToken getRefreshTokenFromResponse(GenericTokenResponse response);

    /**
     * An abstract method for returning the user associated with a request;  This
     * could be based on the contents of the ID Token or it could be returned based on making a call
     * to the user_info or profile endpoint associated with a userr: For example: graph.microsoft.com/me
     * This allows IDPs that do not support OIDC to still be able to return a user to us
     * This method should take the TokenResponse as a parameter
     *
     * @return
     */
    public abstract GenericAccount createAccount(GenericTokenResponse response);

    /**
     * Abstract method for validating the authorization request.  In the case of AAD this is the method
     * from which the details of the authorization request including authority validation would occur (preferred network and preferred cache)
     *
     * @param request
     */
    protected abstract void validateAuthorizationRequest(GenericAuthorizationRequest request);

    /**
     * Abstract method for validating the token request.  Generally speaking I expect this just to be validating
     * that all of the information was provided in the Token Request in order to successfully complete it.
     *
     * @param request
     */
    protected abstract void validateTokenRequest(GenericTokenRequest request);

    /**
     * Abstract method for translating the HttpResponse to a TokenResponse.
     *
     * @param response
     */
    protected abstract GenericTokenResult getTokenResultFromHttpResponse(HttpResponse response);

    // TODO
//    protected abstract void validateAuthorizationResponse(GenericAuthorizationResponse response);

//    protected abstract void validateTokenResponse(GenericTokenResponse response);
}