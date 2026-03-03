// --- Movie ---

export interface Movie {
  id: number;
  title: string;
  name?: string; // TV shows use "name" instead of "title"
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string;
  first_air_date?: string; // TV shows use "first_air_date" instead of "release_date"
  vote_average: number;
  vote_count: number;
  popularity: number;
  genre_ids: number[];
  original_language: string;
  job?: string; // Present in crew credits (e.g. "Director")
  media_type?: 'movie' | 'tv' | 'person';
}

// --- TV Detail ---

export interface TvDetail {
  id: number;
  name: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  first_air_date: string;
  last_air_date: string;
  vote_average: number;
  vote_count: number;
  episode_run_time: number[];
  number_of_seasons: number;
  number_of_episodes: number;
  tagline: string;
  status: string;
  genres: Genre[];
  seasons: TvSeason[];
  networks: TvNetwork[];
  created_by: TvCreator[];
  credits: Credits;
}

export interface TvSeason {
  id: number;
  name: string;
  season_number: number;
  episode_count: number;
  poster_path: string | null;
  air_date: string;
  overview: string;
}

export interface TvNetwork {
  id: number;
  name: string;
  logo_path: string | null;
}

export interface TvCreator {
  id: number;
  name: string;
  profile_path: string | null;
}

export interface MovieListResponse {
  page: number;
  results: Movie[];
  total_pages: number;
  total_results: number;
}

export interface MovieDetail {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string;
  vote_average: number;
  vote_count: number;
  runtime: number;
  tagline: string;
  budget: number;
  revenue: number;
  status: string;
  genres: Genre[];
  credits: Credits;
}

// --- Genre ---

export interface Genre {
  id: number;
  name: string;
}

export interface GenreListResponse {
  genres: Genre[];
}

// --- Credits ---

export interface Credits {
  cast: CastMember[];
  crew: CrewMember[];
}

export interface CastMember {
  id: number;
  name: string;
  character: string;
  profile_path: string | null;
  order: number;
}

export interface CrewMember {
  id: number;
  name: string;
  job: string;
  department: string;
  profile_path: string | null;
}

// --- Person ---

export interface Person {
  id: number;
  name: string;
  profile_path: string | null;
  known_for_department: string;
  popularity: number;
  gender: number; // 0=unknown, 1=female, 2=male, 3=non-binary
  known_for: Movie[];
}

export interface PersonSearchResponse {
  page: number;
  results: Person[];
  total_pages: number;
  total_results: number;
}

export interface PersonCreditsResponse {
  id: number;
  cast: Movie[];
  crew: Movie[];
}

// --- Watch Providers ---

export interface WatchProviders {
  link: string;
  flatrate: Provider[] | null;
  rent: Provider[] | null;
  buy: Provider[] | null;
}

export interface Provider {
  provider_id: number;
  provider_name: string;
  logo_path: string | null;
}

// --- AI Parse ---

export interface AiMovieQuery {
  intent: 'search' | 'recommend' | 'details' | 'unknown';
  type: 'movie' | 'tv' | null;
  query: string | null;
  title: string | null;
  year: number | null;
  genres: string[] | null;
  language: string | null;
  country: string | null;
  platform: string | null;
  sort: 'relevance' | 'rating' | 'popularity' | 'recent' | null;
  include_adult: boolean;
  // Enhanced AI fields
  confidence: 'high' | 'medium' | 'low' | null;
  alternate_titles: string[] | null;
  actors: string[] | null;
  directors: string[] | null;
  keywords: string[] | null;
  search_queries: string[] | null;
  explanation: string | null;
}

export interface AiSearchResponse {
  parsed: AiMovieQuery;
  bestMatch: Movie | null;
  suggestions: Movie[];
  similarMovies: Movie[];
  results: Movie[];
  totalResults: number;
}
