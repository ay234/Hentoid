package me.devsaki.hentoid.activities.sources;

import android.graphics.Bitmap;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import me.devsaki.hentoid.R;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.json.sources.EHentaiGalleryQuery;
import me.devsaki.hentoid.retrofit.sources.EHentaiServer;
import me.devsaki.hentoid.util.FileHelper;
import me.devsaki.hentoid.util.JsonHelper;
import me.devsaki.hentoid.util.Preferences;
import me.devsaki.hentoid.util.network.HttpHelper;
import timber.log.Timber;

/**
 * Created by Robb_w on 2018/04
 * Implements Ex-Hentai source
 */
public class ExHentaiActivity extends BaseWebActivity {

    private static final String[] GALLERY_FILTER = {"exhentai.org/g/[0-9]+/[A-Za-z0-9\\-_]+"};

    Site getStartSite() {
        return Site.EXHENTAI;
    }

    @Override
    protected CustomWebViewClient getWebClient() {
        CustomWebViewClient client = new ExHentaiWebClient(GALLERY_FILTER, this);
        CookieManager.getInstance().setCookie(".exhentai.org", "sl=dm_2");  // Show thumbs in results page ("extended display")
        CookieManager.getInstance().setCookie(".exhentai.org", "nw=1"); // nw=1 (always) avoids the Offensive Content popup (equivalent to clicking the "Never warn me again" link)
        // ExH serves images through hosts that use http connections, which is detected as "mixed content" by the app
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        return client;
    }

    private class ExHentaiWebClient extends CustomWebViewClient {

        ExHentaiWebClient(String[] filter, WebContentListener listener) {
            super(filter, listener);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (url.startsWith("https://exhentai.org")) {
                CookieManager mgr = CookieManager.getInstance();
                String cookiesStr = mgr.getCookie(".exhentai.org");
                if (cookiesStr != null && (!cookiesStr.contains("ipb_member_id=") || cookiesStr.contains("igneous=mystery"))) {
                    mgr.removeAllCookies(null);
                    webView.loadUrl("https://forums.e-hentai.org/index.php?act=Login&CODE=00/");
                    if (cookiesStr.contains("igneous=mystery"))
                        showTooltip(R.string.help_web_incomplete_exh_credentials, true);
                    else
                        showTooltip(R.string.help_web_invalid_exh_credentials, true);
                }
            }

            if (url.startsWith("https://forums.e-hentai.org/index.php")) {
                CookieManager mgr = CookieManager.getInstance();
                String cookiesStr = mgr.getCookie(".e-hentai.org");
                if (cookiesStr != null && cookiesStr.contains("ipb_member_id="))
                    webView.loadUrl("https://exhentai.org/");
            }

            showTooltip(R.string.help_web_exh_account, false);
        }

        // We keep calling the API without using BaseWebActivity.parseResponse
        @Override
        protected WebResourceResponse parseResponse(@NonNull String urlStr, @Nullable Map<String, String> requestHeaders, boolean analyzeForDownload, boolean quickDownload) {
            CookieManager mgr = CookieManager.getInstance();
            String cookiesStr = mgr.getCookie(".exhentai.org");

            String[] galleryUrlParts = urlStr.split("/");
            EHentaiGalleryQuery query = new EHentaiGalleryQuery(galleryUrlParts[4], galleryUrlParts[5]);
            compositeDisposable.add(EHentaiServer.EXHENTAI_API.getGalleryMetadata(query, cookiesStr)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            metadata ->
                            {
                                isHtmlLoaded = true;
                                Content content = metadata.toContent(urlStr, Site.EXHENTAI);
                                Map<String, String> params = new HashMap<>();
                                params.put(HttpHelper.HEADER_COOKIE_KEY, cookiesStr);
                                content.setDownloadParams(JsonHelper.serializeToJson(params, JsonHelper.MAP_STRINGS));
                                listener.onResultReady(content, quickDownload);
                            },
                            throwable -> {
                                Timber.e(throwable, "Error parsing content.");
                                isHtmlLoaded = true;
                                listener.onResultFailed();
                            })
            );
            return null;
        }
    }
}
