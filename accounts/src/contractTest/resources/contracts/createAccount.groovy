package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should create a new account"
    request {
        method 'POST'
        url '/accounts'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            password: "\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS",
            firstName: "Test",
            lastName: "User",
            birthDate: "1990-05-15",
            amount: 1000.00
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1
        ])
    }
}
