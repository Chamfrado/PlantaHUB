import { useEffect, useLayoutEffect } from 'react';
import { useLocation } from 'react-router-dom';

export default function ScrollToTop() {
  const { hash, pathname, search } = useLocation();

  useEffect(() => {
    const previousRestoration = window.history.scrollRestoration;
    window.history.scrollRestoration = 'manual';

    return () => {
      window.history.scrollRestoration = previousRestoration;
    };
  }, []);

  useLayoutEffect(() => {
    if (hash) {
      return;
    }

    scrollPageToTop();
  }, [pathname, search, hash]);

  useEffect(() => {
    if (!hash) {
      const frame = window.requestAnimationFrame(scrollPageToTop);
      return () => window.cancelAnimationFrame(frame);
    }

    const scrollTimer = window.setTimeout(() => {
      const target = document.getElementById(decodeURIComponent(hash.slice(1)));

      if (target) {
        target.scrollIntoView({
          behavior: 'smooth',
          block: 'start',
        });
      }
    }, 40);

    return () => window.clearTimeout(scrollTimer);
  }, [hash, pathname, search]);

  return null;
}

function scrollPageToTop() {
  window.scrollTo(0, 0);
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
}
