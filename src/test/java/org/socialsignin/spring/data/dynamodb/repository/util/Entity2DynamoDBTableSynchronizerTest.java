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
package org.socialsignin.spring.data.dynamodb.repository.util;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import org.socialsignin.spring.data.dynamodb.repository.support.SimpleDynamoDBCrudRepository;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.data.repository.core.RepositoryInformation;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Entity2DynamoDBTableSynchronizerTest<T, ID> {

    private Entity2DynamoDBTableSynchronizer<T, ID> underTest;
    @Mock
    private AmazonDynamoDB amazonDynamoDB;
    @Mock
    private DynamoDBMapper mapper;
    @Mock
    private ProxyFactory factory;
    @Mock
    private RepositoryInformation repositoryInformation;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SimpleDynamoDBCrudRepository<T, ID> repository;
    @Mock
    private DynamoDBEntityInformation<T, ID> entityInformation;

    @BeforeEach
    public void setUp() throws Exception {
        TargetSource targetSource = mock(TargetSource.class);
        when(targetSource.getTarget()).thenReturn(repository);
        when(factory.getTargetSource()).thenReturn(targetSource);

        when(repository.getEntityInformation()).thenReturn(entityInformation);

        when(entityInformation.getDynamoDBTableName()).thenReturn("tableName");

        CreateTableRequest ctr = mock(CreateTableRequest.class);
        Mockito.lenient().when(mapper.generateCreateTableRequest(any())).thenReturn(ctr);
        DeleteTableRequest dtr = mock(DeleteTableRequest.class);
        Mockito.lenient().when(mapper.generateDeleteTableRequest(any())).thenReturn(dtr);

        DescribeTableResult describeResult = mock(DescribeTableResult.class);
        TableDescription description = mock(TableDescription.class);
        Mockito.lenient().when(description.getTableStatus()).thenReturn(TableStatus.ACTIVE.toString());
        Mockito.lenient().when(describeResult.getTable()).thenReturn(description);
        Mockito.lenient().when(amazonDynamoDB.describeTable(any(DescribeTableRequest.class))).thenReturn(describeResult);
    }

    public void setUp(Entity2DDL mode) {
        underTest = new Entity2DynamoDBTableSynchronizer<>(amazonDynamoDB, mapper, mode);
        underTest.postProcess(factory, repositoryInformation);
    }

    public void runContextStart() {
        underTest.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
    }

    public void runContextStop() {
        underTest.onApplicationEvent(new ContextStoppedEvent(applicationContext));
    }

    @Test
    public void testUnmatchedEvent() {
        setUp(Entity2DDL.NONE);

        underTest.onApplicationEvent(new ContextClosedEvent(applicationContext));

        verify(factory).getTargetSource();
        verifyNoMoreInteractions(amazonDynamoDB, mapper, factory, repositoryInformation, applicationContext);
    }

    @Test
    public void testNone() {
        setUp(Entity2DDL.NONE);

        runContextStart();

        runContextStop();

        verify(factory).getTargetSource();
        verifyNoMoreInteractions(amazonDynamoDB, mapper, factory, repositoryInformation, applicationContext);

    }

    @Test
    public void testCreate() {
        setUp(Entity2DDL.CREATE);

        runContextStart();
        verify(amazonDynamoDB).deleteTable(any(DeleteTableRequest.class));
        verify(amazonDynamoDB).createTable(any());
        verify(amazonDynamoDB).describeTable(any(DescribeTableRequest.class));

        runContextStop();
        verify(amazonDynamoDB).deleteTable(any(DeleteTableRequest.class));
        verifyNoMoreInteractions(amazonDynamoDB);
    }

    @Test
    public void testDrop() {
        setUp(Entity2DDL.DROP);

        runContextStart();

        runContextStop();
    }

    @Test
    public void testCreateOnly() {
        setUp(Entity2DDL.CREATE_ONLY);

        runContextStart();
        verify(amazonDynamoDB).createTable(any());
        verify(amazonDynamoDB).describeTable(any(DescribeTableRequest.class));

        runContextStop();
        verifyNoMoreInteractions(amazonDynamoDB);
    }

    @Test
    public void testCreateDrop() {
        setUp(Entity2DDL.CREATE_DROP);

        runContextStart();

        runContextStop();
    }
}
