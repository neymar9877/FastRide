package com.example.bigproject.Repositories;

import okhttp3.OkHttpClient;
import com.google.gson.Gson;

/**
 * Abstract base class for all Repository classes.
 * Holds shared Supabase credentials, HTTP client, and Gson instance.
 * Also defines the generic RepoCallback interface used for async responses.
 */
public class BaseRepo {
    public static final String SUPABASE_URL =
            "https://tqahzqfcudohlzeqfjqw.supabase.co";

    public static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRxYWh6cWZjdWRvaGx6ZXFmanF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwODAwOTQsImV4cCI6MjA3NzY1NjA5NH0.lZnTkW-EVWdu0hbIjRv4T1-nuc83rqBAY1kjMmqc--Y";

    protected static final OkHttpClient client = new OkHttpClient();
    protected static final Gson gson = new Gson();

    /**
     * Task: prevent direct instantiation of BaseRepo.
     * Input: none
     * Output: none
     */
    protected BaseRepo() {}

    /**
     * Generic callback interface for asynchronous repository operations.
     * Used by all Repository methods to return results or errors to the caller.
     *
     * @param <T> the type of the successful result
     */
    public interface RepoCallback<T> {
        /**
         * Task: called when the operation succeeds.
         * Input: result (T) — the returned data
         * Output: none
         */
        void onSuccess(T result);

        /**
         * Task: called when the operation fails.
         * Input: error (Exception) — the exception that occurred
         * Output: none
         */
        void onError(Exception error);
    }
}
