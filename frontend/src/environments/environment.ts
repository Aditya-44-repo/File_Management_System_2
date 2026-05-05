export const environment = {
  production: false,
  // Dynamically select API base URL based on frontend protocol
  get apiBaseUrl() {
    const protocol = window.location.protocol;
    let port = protocol === 'https:' ? '8080' : '8082';
    return `${protocol}//localhost:${port}/api`;
  }
};
// Usage: environment.apiBaseUrl (as a function property)
