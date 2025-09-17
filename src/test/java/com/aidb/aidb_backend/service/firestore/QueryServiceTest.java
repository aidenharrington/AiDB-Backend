package com.aidb.aidb_backend.service.firestore;

import com.aidb.aidb_backend.model.firestore.Query;
import com.aidb.aidb_backend.service.database.firestore.QueryService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryServiceTest {

    @Mock
    private Firestore firestore;

    @InjectMocks
    private QueryService queryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddQuery_success() throws Exception {
        // Arrange
        Query query = new Query();
        CollectionReference collectionReference = mock(CollectionReference.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        ApiFuture<WriteResult> apiFuture = mock(ApiFuture.class);

        when(firestore.collection("queries")).thenReturn(collectionReference);
        when(collectionReference.document()).thenReturn(documentReference);
        when(documentReference.set(any(Query.class))).thenReturn(apiFuture);
        when(apiFuture.get()).thenReturn(mock(WriteResult.class));
        when(documentReference.getId()).thenReturn("generated-id");

        // Act
        Query returnedQuery = queryService.addQuery(query);

        // Assert
        assertEquals("generated-id", returnedQuery.getId());
        verify(documentReference).set(query);
    }

    @Test
    void testGetQuery_queryByIdExists() throws Exception {
        // Arrange
        String queryId = "query-id";
        CollectionReference collectionReference = mock(CollectionReference.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> apiFuture = mock(ApiFuture.class);
        DocumentSnapshot documentSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("queries")).thenReturn(collectionReference);
        when(collectionReference.document(queryId)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(apiFuture);
        when(apiFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        Query expectedQuery = new Query();
        when(documentSnapshot.toObject(Query.class)).thenReturn(expectedQuery);

        // Act
        Query actualQuery = queryService.getQueryById(queryId);

        // Assert
        assertNotNull(actualQuery);
        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    void testGetQuery_queryByIdDoesNotExist() throws Exception {
        // Arrange
        String queryId = "nonexistent-id";
        CollectionReference collectionReference = mock(CollectionReference.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> apiFuture = mock(ApiFuture.class);
        DocumentSnapshot documentSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("queries")).thenReturn(collectionReference);
        when(collectionReference.document(queryId)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(apiFuture);
        when(apiFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act
        Query actualQuery = queryService.getQueryById(queryId);

        // Assert
        assertNull(actualQuery);
    }
}
