import React, {useEffect, useRef} from 'react';

const algoliaExperiencesUrl =
  'https://cdn.jsdelivr.net/npm/@algolia/experiences/dist/experiences.js?appId=9IOL8AD27F&apiKey=1998641ffb9dfd648720f98cd0dba2d0&experienceId=9IOL8AD27F&env=prod';

let searchElement;

export default function SearchBar() {
  const slot = useRef(null);

  useEffect(() => {
    // Docusaurus can remount the navbar on navigation. Keep the Algolia DOM and widget instance together.
    if (searchElement) {
      slot.current.appendChild(searchElement);
      return;
    }

    searchElement = document.createElement('div');
    searchElement.id = 'autocomplete';
    slot.current.appendChild(searchElement);

    const loader = document.createElement('script');
    loader.src = algoliaExperiencesUrl;
    document.head.appendChild(loader);
  }, []);

  return <div ref={slot} />;
}
