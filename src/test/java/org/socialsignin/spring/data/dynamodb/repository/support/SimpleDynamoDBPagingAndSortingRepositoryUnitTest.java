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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DynamoDBSimpleIdRepository}.
 *
 * @author Michael Lavelle
 * @author Sebastian Just
 */
@ExtendWith(MockitoExtension.class)
public class SimpleDynamoDBPagingAndSortingRepositoryUnitTest {

    SimpleDynamoDBPagingAndSortingRepository<User, Long> repoForEntityWithOnlyHashKey;

    SimpleDynamoDBPagingAndSortingRepository<Playlist, PlaylistId> repoForEntityWithHashAndRangeKey;

    @Mock
    DynamoDBOperations dynamoDBOperations;

    private User testUser;

    private Playlist testPlaylist;

    private PlaylistId testPlaylistId;

    @Mock
    EnableScanPermissions mockEnableScanPermissions;

    @Mock
    DynamoDBEntityInformation<User, Long> entityWithOnlyHashKeyInformation;

    @Mock
    DynamoDBEntityInformation<Playlist, PlaylistId> entityWithHashAndRangeKeyInformation;

    @BeforeEach
    public void setUp() {

        testUser = new User();

        testPlaylistId = new PlaylistId();
        testPlaylistId.setUserName("michael");
        testPlaylistId.setPlaylistName("playlist1");

        testPlaylist = new Playlist(testPlaylistId);

        Mockito.lenient().when(entityWithOnlyHashKeyInformation.getJavaType()).thenReturn(User.class);
        Mockito.lenient().when(entityWithOnlyHashKeyInformation.getHashKey(1L)).thenReturn(1L);

        Mockito.lenient().when(entityWithHashAndRangeKeyInformation.getJavaType()).thenReturn(Playlist.class);
        Mockito.lenient().when(entityWithHashAndRangeKeyInformation.getHashKey(testPlaylistId)).thenReturn("michael");
        Mockito.lenient().when(entityWithHashAndRangeKeyInformation.getRangeKey(testPlaylistId)).thenReturn("playlist1");
        Mockito.lenient().when(entityWithHashAndRangeKeyInformation.isRangeKeyAware()).thenReturn(true);

        repoForEntityWithOnlyHashKey = new SimpleDynamoDBPagingAndSortingRepository<>(entityWithOnlyHashKeyInformation,
                dynamoDBOperations, mockEnableScanPermissions);
        repoForEntityWithHashAndRangeKey = new SimpleDynamoDBPagingAndSortingRepository<>(
                entityWithHashAndRangeKeyInformation, dynamoDBOperations, mockEnableScanPermissions);

        Mockito.lenient().when(dynamoDBOperations.load(User.class, 1L)).thenReturn(testUser);
        Mockito.lenient().when(dynamoDBOperations.load(Playlist.class, "michael", "playlist1")).thenReturn(testPlaylist);

    }

    /**
     * @see DATAJPA-177
     */
    @Test //(expected = .class)
    public void throwsExceptionIfEntityWithOnlyHashKeyToDeleteDoesNotExist() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            repoForEntityWithOnlyHashKey.deleteById(4711L);
        });
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
}
