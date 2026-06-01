package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get account by login"
    request {
        method 'GET'
        url '/accounts/test_user'
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            login: "test_user",
            firstName: "Test",
            lastName: "User",
            birthDate: "1990-05-15",
            amount: 1000.00
        ])
    }
}
