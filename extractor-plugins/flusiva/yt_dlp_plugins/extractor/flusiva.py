# SPDX-License-Identifier: GPL-3.0-only
"""Preserve Instagram's media type so photos never mean video thumbnails."""
from yt_dlp.extractor.instagram import InstagramIE as _InstagramIE


class _FlusivaInstagramIE(_InstagramIE, plugin_name='flusiva'):
    def _extract_product_media(self, product_media):
        result = super()._extract_product_media(product_media)
        result['flusiva_media_type'] = (
            'image' if product_media.get('media_type') == 1
            and not product_media.get('video_versions')
            and not product_media.get('video_dash_manifest') else 'video')
        return result
