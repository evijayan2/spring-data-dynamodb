/*
 * Copyright © 2018 spring-data-dynamodb (https://github.com/boostchicken/spring-data-dynamodb)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.socialsignin.spring.data.dynamodb.repository.support;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.socialsignin.spring.data.dynamodb.exception.BatchWriteException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DynamoDBSimpleIdRepository}.
 *
 * @author Michael Lavelle
 * @author Sebastian Just
 */
@ExtendWith(MockitoExtension.class)
public class SimpleDynamoDBCrudRepositoryTest {
//    @Rule
//    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private PaginatedScanList<User> findAllResultMock;
    @Mock
    private DynamoDBOperations dynamoDBOperations;
    @Mock
    private EnableScanPermissions mockEnableScanPermissions;
    @Mock
    private DynamoDBEntityInformation<User, Long> entityWithSimpleIdInformation;
    @Mock
    private DynamoDBEntityInformation<Playlist, PlaylistId> entityWithCompositeIdInformation;

    private User testUser;
    private Playlist testPlaylist;
    private PlaylistId testPlaylistId;

    private SimpleDynamoDBCrudRepository<User, Long> repoForEntityWithOnlyHashKey;
    private SimpleDynamoDBCrudRepository<Playlist, PlaylistId> repoForEntityWithHashAndRangeKey;

    @BeforeEach
    public void setUp() {

        testUser = new User();

        testPlaylistId = new PlaylistId();
        testPlaylistId.setUserName("michael");
        testPlaylistId.setPlaylistName("playlist1");

        testPlaylist = new Playlist(testPlaylistId);

        Mockito.lenient().when(entityWithSimpleIdInformation.getJavaType()).thenReturn(User.class);
        Mockito.lenient().when(entityWithSimpleIdInformation.getHashKey(1L)).thenReturn(1L);

        Mockito.lenient().when(mockEnableScanPermissions.isFindAllUnpaginatedScanEnabled()).thenReturn(true);
        Mockito.lenient().when(mockEnableScanPermissions.isDeleteAllUnpaginatedScanEnabled()).thenReturn(true);
        Mockito.lenient().when(mockEnableScanPermissions.isCountUnpaginatedScanEnabled()).thenReturn(true);

        Mockito.lenient().when(entityWithCompositeIdInformation.getJavaType()).thenReturn(Playlist.class);
        Mockito.lenient().when(entityWithCompositeIdInformation.getHashKey(testPlaylistId)).thenReturn("michael");
        Mockito.lenient().when(entityWithCompositeIdInformation.getRangeKey(testPlaylistId)).thenReturn("playlist1");
        Mockito.lenient().when(entityWithCompositeIdInformation.isRangeKeyAware()).thenReturn(true);

        repoForEntityWithOnlyHashKey = new SimpleDynamoDBCrudRepository<>(entityWithSimpleIdInformation,
                dynamoDBOperations, mockEnableScanPermissions);
        repoForEntityWithHashAndRangeKey = new SimpleDynamoDBCrudRepository<>(entityWithCompositeIdInformation,
                dynamoDBOperations, mockEnableScanPermissions);

        Mockito.lenient().when(dynamoDBOperations.load(User.class, 1L)).thenReturn(testUser);
        Mockito.lenient().when(dynamoDBOperations.load(Playlist.class, "michael", "playlist1")).thenReturn(testPlaylist);

    }

    @Test
    public void deleteById() {
        final long id = ThreadLocalRandom.current().nextLong();
        User testResult = new User();
        testResult.setId(Long.toString(id));

        when(entityWithSimpleIdInformation.getHashKey(id)).thenReturn(id);
        when(dynamoDBOperations.load(User.class, id)).thenReturn(testResult);

        repoForEntityWithOnlyHashKey.deleteById(id);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(dynamoDBOperations).delete(captor.capture());
        assertEquals(Long.toString(id), captor.getValue().getId());
    }

    @Test
    public void deleteEntity() {
        repoForEntityWithOnlyHashKey.delete(testUser);

        verify(dynamoDBOperations).delete(testUser);
    }

    @Test
    public void deleteIterable() {
        repoForEntityWithOnlyHashKey.deleteAll(findAllResultMock);

        verify(dynamoDBOperations).batchDelete(findAllResultMock);
    }

    @Test
    public void deleteAll() {
        when(dynamoDBOperations.scan(eq(User.class), any(DynamoDBScanExpression.class))).thenReturn(findAllResultMock);

        repoForEntityWithOnlyHashKey.deleteAll();
        verify(dynamoDBOperations).batchDelete(findAllResultMock);
    }

    @Test
    public void testFindAll() {
        when(dynamoDBOperations.scan(eq(User.class), any(DynamoDBScanExpression.class))).thenReturn(findAllResultMock);

        List<User> actual = repoForEntityWithOnlyHashKey.findAll();

        assertSame(actual, findAllResultMock);
    }

    /**
     * /**
     *
     * @see DATAJPA-177
     */
    @Test //(expected = EmptyResultDataAccessException.class)
    public void throwsExceptionIfEntityOnlyHashKeyToDeleteDoesNotExist() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            repoForEntityWithOnlyHashKey.deleteById(4711L);
        });
    }

    @Test
    public void testEntityDelete() {
        final long id = ThreadLocalRandom.current().nextLong();
        User entity = new User();
        entity.setId(Long.toString(id));

        repoForEntityWithOnlyHashKey.delete(entity);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(dynamoDBOperations).delete(captor.capture());
        assertEquals(Long.toString(id), captor.getValue().getId());
    }

    @Test
    public void existsEntityWithOnlyHashKey() {
        when(dynamoDBOperations.load(User.class, 1L)).thenReturn(null);

        boolean actual = repoForEntityWithOnlyHashKey.existsById(1L);

        assertFalse(actual);
    }

    @Test
    public void testCount() {
        repoForEntityWithOnlyHashKey.count();

        verify(dynamoDBOperations).count(eq(User.class), any(DynamoDBScanExpression.class));
    }

    @Test
    public void findOneEntityWithOnlyHashKey() {
        Optional<User> user = repoForEntityWithOnlyHashKey.findById(1L);
        Mockito.verify(dynamoDBOperations).load(User.class, 1L);
        assertEquals(testUser, user.get());
    }

    @Test
    public void findOneEntityWithHashAndRangeKey() {
        Optional<Playlist> playlist = repoForEntityWithHashAndRangeKey.findById(testPlaylistId);
        assertEquals(testPlaylist, playlist.get());
    }

    @Test
    public void testSave() {
        final long id = ThreadLocalRandom.current().nextLong();
        User entity = new User();
        entity.setId(Long.toString(id));

        repoForEntityWithOnlyHashKey.save(entity);

        verify(dynamoDBOperations).save(entity);
    }

    /**
     * @see DATAJPA-177
     */
    @Test //(expected = EmptyResultDataAccessException.class)
    public void throwsExceptionIfEntityWithHashAndRangeKeyToDeleteDoesNotExist() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            PlaylistId playlistId = new PlaylistId();
            playlistId.setUserName("someUser");
            playlistId.setPlaylistName("somePlaylistName");

            repoForEntityWithHashAndRangeKey.deleteById(playlistId);
        });
    }

    @Test
    public void testBatchSave() {

        List<User> entities = new ArrayList<>();
        entities.add(new User());
        entities.add(new User());
        when(dynamoDBOperations.batchSave(anyIterable())).thenReturn(Collections.emptyList());

        repoForEntityWithOnlyHashKey.saveAll(entities);

        verify(dynamoDBOperations).batchSave(anyIterable());
    }

    @Test
    public void testBatchSaveFailure() {
        List<FailedBatch> failures = new ArrayList<>();
        FailedBatch e1 = new FailedBatch();
        e1.setException(new Exception("First exception"));
        failures.add(e1);
        FailedBatch e2 = new FailedBatch();
        e2.setException(new Exception("Followup exception"));
        failures.add(e2);

        List<User> entities = new ArrayList<>();
        entities.add(new User());
        entities.add(new User());
        when(dynamoDBOperations.batchSave(anyIterable())).thenReturn(failures);


        Exception exception = assertThrows(BatchWriteException.class, () -> {
            repoForEntityWithOnlyHashKey.saveAll(entities);
        });
        String message = "Processing of entities failed!";
        assertTrue(message.contains(exception.getMessage()));
    }
}
